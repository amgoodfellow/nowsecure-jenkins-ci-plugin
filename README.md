> [!WARNING]
> This plugin is still in development. For the current version, please see NowSecure's [auto-jenkins-plugin](https://github.com/nowsecure/auto-jenkins-plugin)

# nowsecure-jenkins-ci-plugin

NowSecure provides purpose-built, fully automated mobile application security testing (static and dynamic) for your development pipeline.
By testing your mobile application binary post-build from Jenkins, NowSecure ensures comprehensive coverage of newly developed code, third party components, and system dependencies.

NowSecure quickly identifies and details real issues, provides remediation recommendations, and integrates with ticketing systems such as Azure DevOps and Jira.

This integration requires a NowSecure platform license. See <https://www.nowsecure.com> for more information.

## Getting Started

### Dependencies

This Jenkins plugin requires the following plugins:
- Credentials: https://plugins.jenkins.io/credentials/
- Plain Credentials: https://plugins.jenkins.io/plain-credentials/

These plugins are already installed in over 90% of Jenkins instances according to usage statistics, so most consumers of this plugin will not need to explicitly install these.

Note: This plugin will require the minimum Jenkins version as specified by the above two plugins. At the moment, that's version 2.479 requiring Java 17 or Java 21.

### Installation

First, find this extension in the [Jenkins Plugin Marketplace](https://plugins.jenkins.io/)

Then install it following [Jenkin's instructions](https://www.jenkins.io/doc/book/managing/plugins/#installing-a-plugin) on installing marketplace plugins.

**NOTE:** Currently, compatibility is limited to either Windows / Linux running an X64 architecture, or MacOS on ARM.
In order for the extension to work, please make sure you are running on an appropriate `vmImage`.

### Configuration

To add this component to your CI/CD pipeline, the following should be done:

- Get a token from your NowSecure platform instance. More information on this can be found in the [NowSecure Support Portal](https://support.nowsecure.com/hc/en-us/articles/7499657262093-Creating-a-NowSecure-Platform-API-Bearer-Token).
- Identify the ID of the group in NowSecure Platform that you want your assessment to be included in. More information on this can be found in the
  [NowSecure Support Portal](https://support.nowsecure.com/hc/en-us/articles/38057956447757-Retrieve-Reference-and-ID-Numbers-for-API-Use-Task-ID-Group-App-and-Assessment-Ref).
- Add a `StringCredentials` secret as shown in the documentation for the [Plain Credentials Plugin](https://plugins.jenkins.io/plain-credentials/#plugin-content-description).
  Set the `Secret` to the value of the token created above.

## Job Parameters

The NowSecure Azure CI Extension supports the following parameters:


| Name | Description | Default Value |
|------|-------------|---------------|
| `group` | Defines the group reference that is used to trigger assessments. Information on how to get the group reference can be found in the[NowSecure Support Portal](https://support.nowsecure.com/hc/en-us/articles/38057956447757-Retrieve-Reference-and-ID-Numbers-for-API-Use-Task-ID-Group-App-and-Assessment-Ref) | |
| `token` | Defines the token used to communicate with the NowSecure API. This token should be stored as a secret. Information on how to create a token can be found in the [NowSecure Support Portal](https://support.nowsecure.com/hc/en-us/articles/7499657262093-Creating-a-NowSecure-Platform-API-Bearer-Token). | |
| `binary_file` | Defines the path to the mobile application binary to be processed by NowSecure | |
| `ui_host` | Defines the NowSecure base UI to use. This will not change unless you are leveraging a single tenant. | <https://app.nowsecure.com> |
| `api_host` | Defines the NowSecure base API to use. This will not change unless you are leveraging a single tenant. | <https://lab-api.nowsecure.com> |
| `log_level` | Defines the log level set for the NowSecure analysis task. | `info` | | `analysis_type` | Defines the type of analyst that you want to run. Options are `static` for a static only assessment or `full` for both a static and dynamic assessment. | `static` |
| `artifacts_dir` | Defines the directory for nowsecure artifacts to be output to. In the case of the default assessment results would be `./artifacts/nowsecure/assessment.json` | |
| `polling_duration_minutes` | Defines the length of time (in minutes) to poll for job completion. | If `analysis_type` is `static`, 30. If `full`, 60 |
| `minimum_score` | Defines the score under which an assessment will fail | -1 |
