package com.nowsecure.models;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class NowSecureBinary {
    List<String> arguments = new LinkedList<>();
    // Refers to the index of the arguments list that should be masked
    List<Integer> maskedIndices = new LinkedList<>();
    Map<String, String> environmentVariables = Map.of();
    FilePath toolPath;
    FilePath workspace;
    String toolName;

    protected static String getToolName(String arch, String osName) throws IllegalArgumentException {
        String platform, architecture;
        var extension = "";

        if (osName.toLowerCase().contains("windows")) {
            platform = "windows";
            extension = ".exe";
        } else if (osName.toLowerCase().contains("linux")) {
            platform = "linux";
        } else if (osName.toLowerCase().contains("mac")) {
            platform = "darwin";
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported operating system: %s. Currently we support linux/amd64, windows/amd64, and darwin/arm64",
                    osName));
        }

        if (arch.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            architecture = "amd64";
        } else if ("aarch64".equals(arch) || "arm64".equals(arch)) {
            architecture = "arm64";
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported architecture: %s. Currently we support linux/amd64, windows/amd64, and darwin/arm64.",
                    arch));
        }

        if ((platform.equals("windows") || platform.equals("linux")) && architecture.equals("amd64")) {
            return String.format("ns_%s-%s%s", platform, architecture, extension);
        } else if (platform.equals("darwin") && architecture.equals("arm64")) {
            return String.format("ns_%s-%s%s", platform, architecture, extension);
        }

        throw new IllegalArgumentException(String.format(
                "Unsupported platform / architecture: %s / %s. Currently we support linux/amd64, windows/amd64, and darwin/arm64.",
                platform, arch));
    }

    public NowSecureBinary(String arch, String osName, FilePath workspace) throws InterruptedException, IOException {
        this.toolName = NowSecureBinary.getToolName(arch, osName);
        this.toolPath = workspace.child(toolName);
        this.workspace = workspace;
        try (var inputStream = NowSecureBinary.class.getClassLoader().getResourceAsStream(this.toolName)) {
            this.toolPath.copyFrom(inputStream);
        }
        this.toolPath.chmod(0755);

        // this.arguments.add(this.toolPath.getRemote());
        this.arguments.add("dir");
    }

    public NowSecureBinary addArgument(String flag) {
        this.arguments.add(flag);
        return this;
    }

    public NowSecureBinary addEnvVars(Map<String, String> vars) {
        this.environmentVariables = vars;
        return this;
    }

    public NowSecureBinary addArgument(String flag, String value) {
        if (value != null && !StringUtils.isBlank(value)) {
            this.arguments.addAll(List.of(flag, value));
        }
        return this;
    }

    public NowSecureBinary addToken(String value) {
        if (value != null && !StringUtils.isBlank(value)) {
            this.arguments.addAll(List.of("--token", value));
            this.maskedIndices.add(this.arguments.size() - 1);
        }
        return this;
    }

    private boolean[] createMaskedArray() {
        boolean[] maskArray = new boolean[this.arguments.size()];
        for (int i = 0; i < maskedIndices.size(); i++) {
            maskArray[maskedIndices.get(i)] = true;
        }
        return maskArray;
    }

    public ProcStarter startProc(Launcher launcher, TaskListener listener) throws IOException {
        var masks = createMaskedArray();
        return launcher.launch()
                .envs(this.environmentVariables)
                .cmds(this.arguments.get(0))
                .masks(masks)
                .pwd(this.workspace)
                .stdout(listener)
                .stderr(listener.getLogger());
    }
}
