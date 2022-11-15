[![](readme_image.png)](#readme)

<div align="center">

# JFrog Idea Plugin 

![JFrog Extension Marketplace Installs](https://img.shields.io/jetbrains/plugin/d/9834-jfrog?label=Marketplace%20installs&color=blue&style=for-the-badge)

[![Build status](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/jfrog/jfrog-idea-plugin/actions/workflows/test.yml)
[![Marketplace](https://img.shields.io/jetbrains/plugin/v/9834-jfrog)](https://plugins.jetbrains.com/plugin/9834-jfrog)
[![](https://img.shields.io/badge/Docs-%F0%9F%93%96-blue)](https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin)

</div>

JFrog IntelliJ IDEA plugin adds JFrog Xray scanning of Maven, Gradle, Go, Npm, and PyPI project dependencies to your IntelliJ IDEA.
To learn how to use JFrog IntelliJ IDEA plugin, please visit the [JFrog IntelliJ IDEA Plugin User Guide](https://www.jfrog.com/confluence/display/XRAY/IDE+Integration).

# Building and Testing the Sources

To build the plugin sources, please follow these steps:
1. Clone the code from git.
2. Build and create the JFrog IDEA Plugin zip file by running the following gradle command.
After the build finishes, you'll find the zip file in the *plugin/build/distributions* directory, located under the *jfrog-idea-plugin* directory.
The zip file can be loaded into IntelliJ

```
gradle clean build
```

# Developing the Plugin Code
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







