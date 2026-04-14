package com.nowsecure.plugin;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.nowsecure.models.AnalysisType;
import com.nowsecure.models.LogLevel;
import com.nowsecure.models.NowSecureBinary;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class NowSecurePlugin extends Builder implements SimpleBuildStep {
    // Required
    private final String binaryFilePath;
    private final String group;
    // Note: this is a Credential ID, not the actual token
    // For information on Credentials see the following
    // https://github.com/jenkinsci/credentials-plugin/blob/master/docs/consumer.adoc
    private final String tokenCredentialId;

    private String artifactDir;
    private String apiHost;
    private String uiHost;
    private String nowsecureCIVersion;

    private LogLevel logLevel = LogLevel.INFO;
    private AnalysisType analysisType = AnalysisType.STATIC;

    private int minimumScore;
    private int pollingDurationMinutes;

    @DataBoundConstructor
    public NowSecurePlugin(String binaryFilePath, String group, String tokenCredentialId) {
        this.binaryFilePath = Util.fixEmptyAndTrim(binaryFilePath);
        this.group = Util.fixEmptyAndTrim(group);
        this.tokenCredentialId = Util.fixEmptyAndTrim(tokenCredentialId);
    }

    private Map<String, String> getProxyEnvVars(ProxyConfiguration configuration) {
        if (configuration == null) {
            return Map.of();
        }

        final var host = configuration.getName();
        final var port = configuration.getPort();

        final var user = configuration.getUserName();
        final var pass = Secret.toString(configuration.getSecretPassword());

        final var authentication =
                (StringUtils.isEmpty(user) && StringUtils.isEmpty(pass)) ? "" : String.format("%s:%s@", user, pass);

        final var httpProxy = String.format("http://%s%s:%d", authentication, host, port);

        return Map.of("HTTP_PROXY", httpProxy, "HTTPS_PROXY", httpProxy, "NO_PROXY", configuration.getNoProxyHost());
    }

    public String getStringProperty(Computer computer, String propName)
            throws AbortException, InterruptedException, IOException {
        var properties = computer.getSystemProperties();
        if (properties != null && properties.get(propName) instanceof String str) {
            return str;
        } else {
            throw new AbortException(String.format("Unexpected type for system property '%s'", propName));
        }
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        final var worker = workspace.toComputer();
        if (worker == null) {
            throw new AbortException("Workspace not a file on a particular Computer");
        }

        final var arch = getStringProperty(worker, "os.arch");
        final var osName = getStringProperty(worker, "os.name");

        final var binaryFile = workspace.child(binaryFilePath);

        // this method evaluates expressions and tracks usage for the current run - something that was previously done
        // manually
        final var optionalCredentials = Optional.ofNullable(
                CredentialsProvider.findCredentialById(tokenCredentialId, StringCredentials.class, run));

        if (!binaryFile.exists()) {
            var errorMessage = String.format("Cannot find binary file at path: %s", binaryFile.toURI());
            listener.error(errorMessage);
            throw new AbortException(errorMessage);
        }

        if (optionalCredentials.isEmpty()) {
            var errorMessage = "Could not find a TextCredential matching the specified credentialId";
            listener.error(errorMessage);
            throw new AbortException(errorMessage);
        }

        // findCredentialsById already tracked usage against this Run automatically.
        final var credential = optionalCredentials.get();
        final var token = credential.getSecret().getPlainText();

        final var tool = new NowSecureBinary(arch, osName, workspace)
                .addEnvVars(getProxyEnvVars(Jenkins.get().getProxy()))
                .addArgument("run")
                .addArgument("file", binaryFile.getRemote())
                .addArgument("--group-ref", group)
                .addArgument("--api-host", apiHost)
                .addArgument("--ui-host", uiHost)
                .addArgument("--log-level", logLevel.toString().toLowerCase())
                .addArgument("--analysis-type", analysisType.toString().toLowerCase())
                .addArgument("--save-findings")
                .addArgument("--artifacts-dir", artifactDir)
                .addArgument("--output", String.format("%s%sassessment.json", artifactDir, File.separator))
                .addArgument("--minimum-score", String.valueOf(minimumScore))
                .addArgument("--poll-for-minutes", String.valueOf(pollingDurationMinutes))
                .addArgument("--ci-environment", "jenkins")
                .addToken(token);

        final var exitCode = tool.startProc(launcher, listener).join();

        if (exitCode != 0) {
            listener.getLogger().println("Exit Code: " + exitCode);
            throw new AbortException("NowSecure binary finished with nonzero exit code");
        }
    }

    // should be a plugin-unique camel-cased identifier used by workflows
    @Symbol("nowsecureAssessment")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "NowSecure Assessment Configuration";
        }

        // Has to be of the form 'doCheck<FieldName>'
        // The @QueryParameter annotation injects the value from the form field.
        @POST
        public FormValidation doCheckBinaryFile(@QueryParameter String binaryFile) {
            if (StringUtils.isBlank(binaryFile)) {
                return FormValidation.error("Target Filename cannot be empty.");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckGroup(@QueryParameter String group) {
            if (StringUtils.isBlank(group)) {
                return FormValidation.error("Group Ref cannot be empty.");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckTokenCredentialItems(@QueryParameter String tokenCredentialId) {
            if (StringUtils.isBlank(tokenCredentialId)) {
                return FormValidation.error("Token Credential cannot be empty");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckApiHost(@QueryParameter String apiHost) {
            if (!StringUtils.isBlank(apiHost)) {
                try {
                    new URI(apiHost).toURL();
                } catch (Exception e) {
                    return FormValidation.error("Cannot be converted to a valid URL");
                }
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckUiHost(@QueryParameter String uiHost) {
            if (!StringUtils.isBlank(uiHost)) {
                try {
                    new URI(uiHost).toURL();
                } catch (Exception e) {
                    return FormValidation.error("Cannot be converted to a valid URL");
                }
            }
            return FormValidation.ok();
        }

        // Has to be of the form 'doCheck<FieldName>' to match the tokenCredentialId field.
        @POST
        public FormValidation doCheckTokenCredentialId(@AncestorInPath Item item, @QueryParameter String value) {
            // Item represents the job or folder the form is within. A `null` value means that we're in the global
            // context
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Token Credential cannot be empty");
            }
            if (value.startsWith("${") && value.endsWith("}")) {
                return FormValidation.warning("Cannot validate expression-based credentials");
            }
            // 'listCredentialsInItem' don't access the underlying credential value
            // so don't count as credential store usage during form validation
            boolean found = item != null
                    ? !CredentialsProvider.listCredentialsInItem(
                                    StringCredentials.class,
                                    item,
                                    hudson.security.ACL.SYSTEM2,
                                    Collections.emptyList(),
                                    CredentialsMatchers.withId(value))
                            .isEmpty()
                    : !CredentialsProvider.listCredentialsInItemGroup(
                                    StringCredentials.class,
                                    Jenkins.get(),
                                    hudson.security.ACL.SYSTEM2,
                                    Collections.emptyList(),
                                    CredentialsMatchers.withId(value))
                            .isEmpty();
            if (!found) {
                return FormValidation.error("Cannot find the selected credentials");
            }
            return FormValidation.ok();
        }

        // Has to be of the form 'doFill<FieldName>Items' to match the tokenCredentialId field.
        @POST
        public ListBoxModel doFillTokenCredentialIdItems(
                @AncestorInPath Item item, @QueryParameter String tokenCredentialId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(tokenCredentialId);
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(tokenCredentialId);
            }
            return item != null
                    ? fillCredentials(result, item, tokenCredentialId)
                    : fillCredentials(result, Jenkins.get(), tokenCredentialId);
        }

        private static ListBoxModel fillCredentials(StandardListBoxModel result, Item item, String currentValue) {
            return result.includeMatchingAs(
                            hudson.security.ACL.SYSTEM2,
                            item,
                            StringCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.always())
                    .includeCurrentValue(currentValue);
        }

        private static ListBoxModel fillCredentials(
                StandardListBoxModel result, ItemGroup<?> context, String currentValue) {
            return result.includeMatchingAs(
                            hudson.security.ACL.SYSTEM2,
                            context,
                            StringCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.always())
                    .includeCurrentValue(currentValue);
        }
    }

    @DataBoundSetter
    public void setAnalysisType(AnalysisType analysisType) {
        this.analysisType = analysisType;
    }

    @DataBoundSetter
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @DataBoundSetter
    public void setArtifactDir(String artifactDir) {
        var fixed = Util.fixEmptyAndTrim(artifactDir);
        if (fixed != null) {
            this.artifactDir = fixed;
        }
    }

    @DataBoundSetter
    public void setApiHost(String apiHost) {
        var fixed = Util.fixEmptyAndTrim(apiHost);
        if (fixed != null) {
            this.apiHost = fixed;
        }
    }

    @DataBoundSetter
    public void setUiHost(String uiHost) {
        var fixed = Util.fixEmptyAndTrim(uiHost);
        if (fixed != null) {
            this.uiHost = fixed;
        }
    }

    @DataBoundSetter
    public void setNowsecureCIVersion(String nowsecureCIVersion) {
        this.nowsecureCIVersion = nowsecureCIVersion;
    }

    @DataBoundSetter
    public void setMinimumScore(int minimumScore) {
        this.minimumScore = minimumScore;
    }

    @DataBoundSetter
    public void setPollingDurationMinutes(int pollingDurationMinutes) {
        this.pollingDurationMinutes = pollingDurationMinutes;
    }

    public String getBinaryFilePath() {
        return binaryFilePath;
    }

    public String getGroup() {
        return group;
    }

    public String getTokenCredentialId() {
        return tokenCredentialId;
    }

    public String getArtifactDir() {
        return artifactDir;
    }

    public String getApiHost() {
        return apiHost;
    }

    public String getUiHost() {
        return uiHost;
    }

    public String getNowsecureCIVersion() {
        return nowsecureCIVersion;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public int getMinimumScore() {
        return minimumScore;
    }

    public int getPollingDurationMinutes() {
        return pollingDurationMinutes;
    }
}
