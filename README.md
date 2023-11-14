[![](readme-resources/readme_image.png)](#readme)

<div align="center">

# JFrog IntelliJ IDEA Plugin

![JFrog IntelliJ IDEA Plugin Marketplace Installs](https://img.shields.io/jetbrains/plugin/d/9834-jfrog?label=Marketplace%20installs&color=blue&style=for-the-badge)

[![Scanned by Frogbot](https://raw.github.com/jfrog/frogbot/master/images/frogbot-badge.svg)](https://github.com/jfrog/frogbot#readme)
[![Build status](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml)
[![Marketplace](https://img.shields.io/jetbrains/plugin/v/9834-jfrog)](https://plugins.jetbrains.com/plugin/9834-jfrog)

</div>

# 🤖 About this Plugin

The plugin allows developers to find and fix security vulnerabilities in their projects and to see valuable information
about the status of their code by continuously scanning it locally with [JFrog Security](https://jfrog.com/xray/).

### What security capabilities do we provide?
#### Basic
<details>
  <summary>Software Composition Analysis (SCA)</summary>
Scans your project dependencies for security issues and shows you which dependencies are vulnerable. If the vulnerabilities have a fix, you can upgrade to the version with the fix in a click of a button.
</details>

<details>
  <summary>CVE Research and Enrichment</summary>
For selected security issues, get leverage-enhanced CVE data that is provided by our JFrog Security Research team.
Prioritize the CVEs based on:

- **JFrog Severity**: The severity given by the JFrog Security Research team after the manual analysis of the CVE by the team.
CVEs with the highest JFrog security severity are the most likely to be used by real-world attackers.
This means that you should put effort into fixing them as soon as possible.
- **Research Summary**: The summary that is based on JFrog's security analysis of the security issue provides detailed technical information on the specific conditions for the CVE to be applicable.
- **Remediation**: Detailed fix and mitigation options for the CVEs

You can learn more about enriched CVEs [here](https://jfrog.com/help/r/jfrog-security-documentation/jfrog-security-cve-research-and-enrichment).

Check out what our research team is up to and stay updated on newly discovered issues by clicking on this link: <https://research.jfrog.com>
</details>

#### Advanced
*Requires Xray version 3.66.5 or above and Enterprise X / Enterprise+ subscription with [Advanced DevSecOps](https://jfrog.com/xray/#xray-advanced)).*

<details>
  <summary>CVEs Contextual Analysis</summary>
Uses the code context to eliminate false positive reports on vulnerable dependencies that are not applicable to the code. 
CVEs Contextual Analysis is currently supported for Python, Java and JavaScript code.
</details>

<details>
  <summary>Secrets Detection</summary>
Prevents the exposure of keys or credentials that are stored in your source code.
</details>

<details>
  <summary>Infrastructure as Code (IaC) Scan</summary>
Secures your IaC files. Critical to keeping your cloud deployment safe and secure.
</details>

#### Additional Perks

- Security issues are easily visible inline.
- The results show issues with context, impact, and remediation.
- View all security issues in one place, in the JFrog tab.
- For Security issues with an available fixed version, you can upgrade to the fixed version within the plugin.
- Track the status of the code while it is being built, tested, and scanned on the CI server.

In addition to IntelliJ IDEA, the plugin also supports the following IDEs:

- WebStorm
- PyCharm
- Android Studio
- GoLand

# 🏁 Getting Started

Read the the [documentation](https://docs.jfrog-applications.jfrog.io/jfrog-applications/ide/jetbrains-ides) to get started.

# 🔥 Reporting Issues

Please report issues by opening an issue on [GitHub](https://github.com/jfrog/jfrog-idea-plugin/issues).

# 💻 Contributions

We welcome community contribution through pull requests. To help us improve this project, please read our [Contribution](./CONTRIBUTING.md#guidelines) guide.

# 🥏 Release Notes

The release notes are available on [Marketplace](https://plugins.jetbrains.com/plugin/9834-jfrog/versions).
