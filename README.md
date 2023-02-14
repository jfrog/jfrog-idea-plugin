[![](readme_image.png)](#readme)

<div align="center">

# JFrog IntelliJ IDEA Plugin 

![JFrog IntelliJ IDEA Plugin Marketplace Installs](https://img.shields.io/jetbrains/plugin/d/9834-jfrog?label=Marketplace%20installs&color=blue&style=for-the-badge)

[![Build status](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml)
[![Marketplace](https://img.shields.io/jetbrains/plugin/v/9834-jfrog)](https://plugins.jetbrains.com/plugin/9834-jfrog)
[![](https://img.shields.io/badge/Docs-%F0%9F%93%96-blue)](https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin)

</div>

## About this Plugin
The cost of remediating a vulnerability is akin to the cost of fixing a bug.
The earlier you remediate a vulnerability in the release cycle, the lower the cost.
[JFrog Xray](https://jfrog.com/xray/) is instrumental in flagging components when vulnerabilities are discovered in production systems at runtime,
or even sooner, during the development.

JFrog IntelliJ IDEA Plugin adds JFrog Xray scanning of project dependencies to IntelliJ IDEA.
It allows developers to view panels displaying vulnerability information about the components and their dependencies directly in their IDE.
The plugin also allows developers to track the status of the code while it is being built, tested and scanned on the CI server.

Currently, Maven, Gradle, npm, Yarn, Python and Go are supported by the plugin.

In addition to IntelliJ IDEA, the plugin also supports the following IDEs:
* WebStorm
* PyCharm
* Android Studio
* GoLand

## Getting Started
1. Install the JFrog IntelliJ IDEA Plugin: <iframe frameborder="none" width="245px" height="48px" src="https://plugins.jetbrains.com/embeddable/install/9834"></iframe>
2. Need a FREE JFrog environment in the cloud? [Create one now](#set-up-a-free-jfrog-environment-in-the-cloud).
3. [Connect the plugin to your JFrog environment](#connecting-to-your-jfrog-environment).
4. [Start](#using-the-plugin) using the plugin.

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

# Code Contributions
We welcome community contribution through pull requests.

# Release Notes
The release are available on [Marketplace](https://plugins.jetbrains.com/plugin/9834-jfrog/versions).
