# nowsecure-jenkins-ci-plugin

NowSecure provides purpose-built, fully automated mobile application security testing (static and dynamic) for your development pipeline.
By testing your mobile application binary post-build from Jenkins, NowSecure ensures comprehensive coverage of newly developed code, third party components, and system dependencies.

NowSecure quickly identifies and details real issues, provides remediation recommendations, and integrates with ticketing systems such as Azure DevOps and Jira.

This integration requires a NowSecure platform license. See <https://www.nowsecure.com> for more information.


## Getting Started

### Installation

Find this plugin in the [Jenkins Plugin Marketplace](https://plugins.jenkins.io/) and install it following [Jenkins' instructions](https://www.jenkins.io/doc/book/managing/plugins/#installing-a-plugin).

> [!NOTE] This plugin requires a worker node running Linux (x86_64), Windows (x86_64), or macOS (Apple Silicon). Builds dispatched to other architectures will fail.

### Configuration

### 1. Create a NowSecure API token

Generate an API token from your NowSecure platform instance. See the
[NowSecure Support Portal][create-token] for instructions.

[create-token]: https://support.nowsecure.com/hc/en-us/articles/7499657262093-Creating-a-NowSecure-Platform-API-Bearer-Token

### 2. Add the token as a Jenkins credential

Add the token as a **Secret Text** credential in Jenkins following the [Plain Credentials Plugin documentation](https://plugins.jenkins.io/plain-credentials/#plugin-content-description). Note the credential ID you assign — you will need it in the next step.

### 3. Get Your NowSecure Group ID

Identify the ID of the NowSecure Platform group you want assessments to be associated with. See the [NowSecure Support Portal](retrieve-ids) for instructions.

[retrieve-ids]: https://support.nowsecure.com/hc/en-us/articles/38057956447757-Retrieve-Reference-and-ID-Numbers-for-API-Use-Task-ID-Group-App-and-Assessment-Ref
### 4. Add the build step

Add the `NowSecure Assessment Configuration` build step to your job and fill in the required fields.

## Parameter Reference

### Required

| Name | Description |
|------|-------------|
| `binaryFilePath` | Path to the mobile application binary (`.ipa` or `.apk`) relative to the workspace root. |
| `group` | The NowSecure group reference ID to associate the assessment with. See the [NowSecure Support Portal](https://support.nowsecure.com/hc/en-us/articles/38057956447757-Retrieve-Reference-and-ID-Numbers-for-API-Use-Task-ID-Group-App-and-Assessment-Ref) for how to find this value. |
| `tokenCredentialId` | The Jenkins credential ID of the **Secret Text** credential containing your NowSecure API bearer token. |

### Optional

| Name | Description | Default |
|------|-------------|---------|
| `artifactDir` | Directory (relative to workspace) where NowSecure output files are written. The assessment result JSON will be at `<artifactDir>/assessment.json`. | `nowsecure` |
| `analysisType` | Type of assessment to run. `STATIC` runs static analysis only; `FULL` runs both static and dynamic analysis. | `STATIC` |
| `minimumScore` | The assessment score below which the build will be marked as failed. Set to `0` to disable score gating. | `0` |
| `pollingDurationMinutes` | How long (in minutes) to wait for the assessment to complete before timing out. | `20` |
| `apiHost` | NowSecure API base URL. Only change this if you are on a single-tenant NowSecure instance. | `https://lab-api.nowsecure.com` |
| `uiHost` | NowSecure UI base URL. Only change this if you are on a single-tenant NowSecure instance. | `https://app.nowsecure.com` |
| `logLevel` | Log verbosity for the NowSecure assessment task. One of `DEBUG`, `INFO`, `WARN`, `ERROR`. | `INFO` |

