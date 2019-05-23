[![Build status](https://ci.appveyor.com/api/projects/status/vypm9uua3vf3plan?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/jfrog-idea-plugin) [![Build Status](https://travis-ci.org/jfrog/jfrog-idea-plugin.svg?branch=master)](https://travis-ci.org/jfrog/jfrog-idea-plugin)
# JFrog Idea Plugin 

JFrog IntelliJ IDEA plugin adds JFrog Xray scanning of Maven, Gradle and Npm project dependencies to your IntelliJ IDEA.

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

# Using JFrog IntelliJ IDEA plugin
To learn how to use JFrog IntelliJ IDEA plugin, please visit the [JFrog IntelliJ IDEA Plugin User Guide](https://www.jfrog.com/confluence/display/XRAY/IDE+Integration).

# Release Notes
The release are available on [Bintray](https://bintray.com/jfrog/jfrog-jars/jfrog-idea-plugin#release).
