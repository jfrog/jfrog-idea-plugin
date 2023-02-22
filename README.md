[![](readme-resources/readme_image.png)](#readme)

<div align="center">

# JFrog IntelliJ IDEA Plugin 

![JFrog IntelliJ IDEA Plugin Marketplace Installs](https://img.shields.io/jetbrains/plugin/d/9834-jfrog?label=Marketplace%20installs&color=blue&style=for-the-badge)

[![Build status](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml)
[![Marketplace](https://img.shields.io/jetbrains/plugin/v/9834-jfrog)](https://plugins.jetbrains.com/plugin/9834-jfrog)

</div>

# Table of Contents
- [About this Plugin](#about-this-plugin)
- [Supported Packages](#supported-packages)
- [Getting Started](#getting-started)
  - [Connecting to Your JFrog Environment](#connecting-to-your-jfrog-environment)
  - [Apply Xray Policies](#apply-xray-policies)
- [Using the Plugin](#using-the-plugin)
  - [The Local View](#the-local-view)
      - [Scanning a Project](#scanning-a-project)
      - [Viewing Vulnerability Details](#viewing-vulnerability-details)
      - [Contextual Analysis](#contextual-analysis)
      - [Severity Icons](#severity-icons)
  - [The CI View](#the-ci-view)
      - [How Does It Work?](#how-does-it-work)
      - [Setting Up CI Integration](#setting-up-ci-integration)
- [Troubleshooting](#troubleshooting)
- [Reporting Issues](#reporting-issues)
- [Contributions](#contributions)
  - [Building and Testing the Sources](#building-and-testing-the-sources)
  - [Developing the Plugin Code](#developing-the-plugin-code)
  - [Code Contributions](#code-contributions)
- [Release Notes](#release-notes)

# About this Plugin
The plugin allows developers to find and fix security vulnerabilities in their projects and to see valuable information
about the status of their code by continuously scanning it locally with [JFrog Xray](https://jfrog.com/xray/).

### What security capabilities do we provide?
#### Software Composition Analysis (SCA)
Scan your project dependencies for security issues.

#### CVE Research and Enrichment
For selected security issues, get leverage-enhanced CVE data that is provided by our JFrog Security Research team.
Prioritize the CVEs based on:
* **JFrog Severity**: The severity given by the JFrog Security Research team after the manual analysis of the CVE by the team.
CVEs with the highest JFrog security severity are the most likely to be used by real-world attackers.
This means that you should put effort into fixing them as soon as possible.
* **Research Summary**: The summary that is based on JFrog's security analysis of the security issue provides detailed technical information on the specific conditions for the CVE to be applicable.
* **Remediation**: Detailed fix and mitigation options for the CVEs

You can learn more about enriched CVEs [here](https://www.jfrog.com/confluence/display/JFROG/JFrog+Security+CVE+Research+and+Enrichment).

Check out what our research team is up to and stay updated on newly discovered issues by clicking on this link: https://research.jfrog.com

#### Advanced Scans
*Requires Enterprise X / Enterprise+ subscription with Advanced DevSecOps.*

With advanced [**Contextual Analysis**](#contextual-analysis), understand the applicability of CVEs in your application and utilize JFrog Security scanners to analyze the way you use 3rd party packages in your projects.
Automatically validate some high-impact vulnerabilities, such as vulnerabilities that have prerequisites for exploitations, and reduce false positives and vulnerability noise with smart CVE analysis.

To learn more, see [here](https://www.jfrog.com/confluence/display/JFROG/Vulnerability+Contextual+Analysis).

#### Additional Perks
* Security issues are easily visible inline.
* The results show issues with context, impact, and remediation.
* View all security issues in one place, in the JFrog tab.
* For Security issues with an available fixed version, you can upgrade to the fixed version within the plugin.
* Track the status of the code while it is being built, tested, and scanned on the CI server.

In addition to IntelliJ IDEA, the plugin also supports the following IDEs:
* WebStorm
* PyCharm
* Android Studio
* GoLand

# Supported Packages
| Features                                          | Go  | Maven | Gradle | npm | Yarn v1 | Python |
|---------------------------------------------------|:---:|:-----:|:------:|:---:|:-------:|:------:|
| SCA                                               |  ✅  |   ✅   |   ✅    |  ✅  |    ✅    |   ✅    |
| CVE Research and Enrichment                       |  ✅  |   ✅   |   ✅    |  ✅  |    ✅    |   ✅    |
| Upgrade vulnerable dependencies to fixed versions |  ✅  |   ✅   |   ❌    |  ✅  |    ✅    |   ❌    |
| Contextual Analysis                               |  ❌  |   ❌   |   ❌    |  ✅  |    ❌    |   ✅    |

# Getting Started
1. Install the JFrog IntelliJ IDEA Plugin via the Plugins tab in the IDE settings, or in [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/9834-jfrog).
2. [Connect the plugin to your JFrog environment](#connecting-to-your-jfrog-environment).
3. [Start](#using-the-plugin) using the plugin.

## Connecting to Your JFrog Environment
<details>
  <summary>Set Up a FREE JFrog Environment in the Cloud</summary>

Need a FREE JFrog environment in the Cloud, so that JFrog IntelliJ IDEA Plugin can connect to it? Just run one of the following commands in your terminal. The commands will do the following:

1. Install JFrog CLI on your machine.
2. Create a FREE JFrog environment in the Cloud for you.
3. Configure IntelliJ IDEA to connect to your new environment.

**MacOS and Linux using cURL**
```
curl -fL https://getcli.jfrog.io?setup | sh
```

**Windows using PowerShell**
```
powershell "Start-Process -Wait -Verb RunAs powershell '-NoProfile iwr https://releases.jfrog.io/artifactory/jfrog-cli/v2-jf/[RELEASE]/jfrog-cli-windows-amd64/jf.exe -OutFile $env:SYSTEMROOT\system32\jf.exe'" ; jf setup
```
</details>

<details>
  <summary>Connect the Plugin to an Existing JFrog Environment</summary>

You can connect the plugin to your JFrog environment:

<details>
  <summary>In the IDE Settings</summary>
  Once the plugin is successfully installed, connect the plugin to your instance of the JFrog Platform:

  1. If your JFrog Platform instance is behind an HTTP proxy, configure the proxy settings as described [here](https://www.jetbrains.com/help/idea/settings-http-proxy.html).
     Manual proxy configuration is supported since version 1.3.0 of the JFrog IntelliJ IDEA Plugin. Auto-detect proxy settings is supported since version 1.7.0.
  2. Under **Settings (Preferences)** | **Other Settings**, click **JFrog Global Configuration**.
  3. Set your JFrog Platform URL and login credentials.
  4. Test your connection to Xray using the Test Connection button.
  
  ![](readme-resources/connect.png)
</details>

<details>
  <summary>Using Environment Variables</summary>
The plugin also supports connecting to your JFrog environment using environment variables:

1. Under **Settings (Preferences)** | **Other Settings**, click **JFrog Global Configuration**.
2. Mark **Load connection details from environment variables**.

You may provide basic auth credentials or access token as follows:

*Note: For security reasons, it is recommended to unset the environment variables after launching the IDE.*

- `JFROG_IDE_PLATFORM_URL` - JFrog Platform URL
- `JFROG_IDE_USERNAME` - JFrog Platform username
- `JFROG_IDE_PASSWORD` - JFrog Platform password
- `JFROG_IDE_ACCESS_TOKEN` - JFrog Platform access token
</details>
</details>

**Notes:**
* If your JFrog Platform instance uses a domain with a self-signed certificate, add the certificate to IDEA as described [here](https://www.jetbrains.com/help/idea/settings-tools-server-certificates.html).
* From JFrog Xray version **1.9** to **2.x**, IntelliJ IDEA users connecting to Xray from IntelliJ are required to be granted the ‘View Components’ action in Xray.
* From JFrog Xray version **3.x**, as part of the JFrog Platform, IntelliJ IDEA users connecting to Xray from IntelliJ require ‘Read’ permission. For more information, see [here](https://www.jfrog.com/confluence/display/JFROG/Permissions).

## Apply Xray Policies
You can configure the JFrog IntelliJ IDEA Plugin to use the security policies you create in Xray.
Policies enable you to create a set of rules, in which each rule defines security criteria, with a corresponding set of automatic actions according to your needs.
Policies are enforced when applying them to Watches.

If you'd like to use a JFrog Project that is associated with the policy, follow these steps:
1. Create a [JFrog Project](https://www.jfrog.com/confluence/display/JFROG/Projects), or obtain the relevant JFrog Project key.
2. Create a [Policy](https://www.jfrog.com/confluence/display/JFROG/Creating+Xray+Policies+and+Rules) on JFrog Xray.
3. Create a [Watch](https://www.jfrog.com/confluence/display/JFROG/Configuring+Xray+Watches) on JFrog Xray and assign your Policy and Project as resources to it.
4. Configure your Project key in the plugin settings: under **Settings (Preferences)** | **Other Settings**, click **JFrog Global Configuration** and go to the **Settings** tab.

If however your policies are referenced through Xray Watches, follow these steps instead:
1. Create one or more [Watches](https://www.jfrog.com/confluence/display/JFROG/Configuring+Xray+Watches) on JFrog Xray.
2. Configure your Watches in the plugin settings: under **Settings (Preferences)** | **Other Settings**, click **JFrog Global Configuration** and go to the **Settings** tab.

# Using the Plugin
After the JFrog Plugin is installed, a new JFrog panel is added at the bottom of the screen.
Opening the JFrog panel displays two views:

- The **Local** view displays information about the local code as it is being developed in the IDE.
  JFrog Xray continuously scans the project's dependencies and source code locally.
  The information is displayed in the **Local** view.
- The **CI** view allows the tracking of the code as it is built, tested and scanned by the CI server.
  It displays information about the status of the build and includes a link to the build log on the CI server.

## The Local View
The JFrog IntelliJ IDEA Plugin continuously scans your project's dependencies with JFrog Xray and displays this information under the Local view.
It allows developers to view vulnerability information about their dependencies and source code in their IDE.
With this information, a developer can make an informed decision on whether to use a component or not before it gets entrenched into the organization’s product.

### Scanning a Project
Scan your project by clicking the Run Scan ![](readme-resources/run-scan-button.png) button.
After the scan is done, a list of vulnerable files will appear.

Each descriptor file (like pom.xml in Maven, go.mod in Go, etc.) in the list contains vulnerable dependencies, and each dependency contains the vulnerabilities themselves.

By right-clicking on a dependency line, you can jump to the dependency's declaration in the descriptor file (if it's a direct dependency), or to direct dependencies that depend on the vulnerable component (if any).

![](readme-resources/jump-to-descriptor.png)

By right-clicking on a vulnerability line, you can create an [Ignore Rule](https://www.jfrog.com/confluence/display/JFROG/Ignore+Rules) in Xray.

*Creating Ignore Rules is only available [when a JFrog Project or Watch is defined](#apply-xray-policies).*

![](readme-resources/create-ignore-rule.png)

### Viewing Vulnerability Details
Clicking a vulnerability in the list will open the vulnerability details view.
This view contains information about the vulnerability, the vulnerable component, fixed versions, impact paths and much more.

![](readme-resources/vuln-details.png)
![](readme-resources/vuln-impact-graph.png)

### Contextual Analysis
*Requires Enterprise X / Enterprise+ subscription with Advanced DevSecOps.*

You can avoid wasting your time fixing risks that are not imposed and reduce false positives with contextual analysis.
As part of the project's scan, JFrog IntelliJ IDEA Plugin will let you know if vulnerabilities in your dependencies are applicable or not.

![](readme-resources/not-applicable.png)
![](readme-resources/applicable.png)

### Severity Icons
The icon demonstrates the top severity issue of a selected component and its transitive dependencies. The following table describes the severities from highest to lowest:

|                                                                                                                                                                                                   Icon                                                                                                                                                                                                    |                Severity                |
|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:--------------------------------------:|
|                                                                                                                                                                       <img src="src/main/resources/icons/critical.svg" width="20">                                                                                                                                                                        |                Critical                |
|                                                                                                                                                                         <img src="src/main/resources/icons/high.svg" width="20">                                                                                                                                                                          |                  High                  |
|                                                                                                                                                                        <img src="src/main/resources/icons/medium.svg" width="20">                                                                                                                                                                         |                 Medium                 |
|                                                                                                                                                                          <img src="src/main/resources/icons/low.svg" width="20">                                                                                                                                                                          |                  Low                   |
|                                                                                                                                                                        <img src="src/main/resources/icons/unknown.svg" width="20">                                                                                                                                                                        |                Unknown                 |
| <img src="src/main/resources/icons/unknownnotapplic.svg" height="15" width="15"><img src="src/main/resources/icons/lownotapplic.svg" height="15" width="15"><img src="src/main/resources/icons/mediumnotapplic.svg" height="15" width="15"><img src="src/main/resources/icons/highnotapplic.svg" height="15" width="15"><img src="src/main/resources/icons/criticalnotapplic.svg" height="15" width="15"> | [Not Applicable](#contextual-analysis) |

## The CI View
The JFrog IntelliJ IDEA Plugin allows you to view information about your builds directly from your CI system.
This allows developers to keep track of the status of their code, while it is being built, tested and scanned as part of the CI pipeline, regardless of the CI provider used.

This information can be viewed inside IntelliJ IDEA, from the **JFrog** Panel, under the **CI** tab.

The following details can be made available in the CI view:

* Status of the build run (passed or failed)
* Build run start time
* Git branch and latest commit message
* Link to the CI run log
* Security information about the build artifacts and dependencies

![](readme-resources/ci-view.png)

### How Does It Work?
The CI information displayed in IDEA is pulled by the JFrog IDEA Plugin directly from JFrog Artifactory.
This information is stored in Artifactory as part of the build-info, which is published to Artifactory by the CI server.
Read more about build-info in the [Build Integration](https://www.jfrog.com/confluence/display/JFROG/Build+Integration) documentation page.
If the CI pipeline is also configured to scan the build-info by JFrog Xray, the JFrog IDEA Plugin will pull the results of the scan from JFrog Xray and display them in the CI view as well.

### Setting Up CI Integration
Set up your CI pipeline to expose information, so that it is visible in IDEA as described [here](https://www.jfrog.com/confluence/display/JFROG/Setting+Up+CI+Integration).

Next, follow these steps:
1. Under **Settings (Preferences)** | **Other Settings**, click **JFrog Global Configuration**. configure the JFrog Platform URL and the user you created.
2. Under **Settings (Preferences)** | **Other Settings**, click **JFrog CI Integration**. Set your CI build name in the **Build name pattern** field. This is the name of the build published to Artifactory by your CI pipeline. You have the option of setting * to view all the builds published to Artifactory.
   ![](readme-resources/ci-settings.png)
3. Click **Apply** and open the **CI** tab under the **JFrog** panel at the bottom of the screen and click the **Refresh** button.

# Troubleshooting
The JFrog IntelliJ IDES Plugin uses the IntelliJ IDEA log files. By default, the log level used by the plugin is INFO.

You have the option of increasing the log level to DEBUG. Here's how to do it:

1. Go to **Help** | **Diagnostic Tools** | **Debug Log Settings...**
2. Inside the **Custom Debug Log Configuration** window add the following line:
```
#com.jfrog.ide.idea.log.Logger
```
To see the Intellij IDEA log file, depends on the IDE version and OS as described [here](https://intellij-support.jetbrains.com/hc/en-us/articles/207241085-Locating-IDE-log-files), go to **Help** | **Show/reveal Log in Explorer/finder/Konqueror/Nautilus**.

# Reporting Issues

Please report issues by opening an issue on [Github](https://github.com/jfrog/jfrog-idea-plugin/issues).

# Contributions
## Building and Testing the Sources

To build the plugin sources, please follow these steps:
1. Clone the code from git.
2. Build and create the JFrog IDEA Plugin zip file by running the following gradle command.
After the build finishes, you'll find the zip file in the *plugin/build/distributions* directory, located under the *jfrog-idea-plugin* directory.
The zip file can be loaded into IntelliJ

```
gradle clean build
```

## Developing the Plugin Code
If you'd like to help us develop and enhance the plugin, this section is for you.
To build and run the plugin following your code changes, follow these steps:

1. From IntelliJ, open the plugin project, by selecting *jfrog-idea-plugin/build.gradle* file.
2. Build the sources and launch the plugin by the following these steps:
* From the *Gradle Projects* window, expand *Idea --> Tasks -->  IntelliJ*
* Run the *buildPlugin* task.
* Run the *runIdea* task.

## Code Contributions
We welcome community contribution through pull requests.

# Release Notes
The release notes are available on [Marketplace](https://plugins.jetbrains.com/plugin/9834-jfrog/versions).
