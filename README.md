# Overview
JFrog IntelliJ IDEA plugin adds JFrog Xray scanning of Maven project dependencies to your IntelliJ IDEA.

# Getting started

### Prerequisites
IntelliJ IDEA version 2016.2 and above.

JFrog Xray version 1.7.2.3 and above.

### Installation
From IntelliJ IDEA:

Go to Settings (Preferences) -> Plugins -> Browse repositories -> Search for JFrog -> Install
![Alt text](docs/install.png?raw=true "Installing JFrog plugin")

### User Guide

#### Setting up JFrog Xray
Go to Settings (Preferences) -> Other Settings -> JFrog Xray Configuration

Configure JFrog Xray URL and credentials.

Test your connection to Xray using the ```Test connection``` button.

![Alt text](docs/credentials.png?raw=true "Setting up credentials")

#### View
The JFrog IntelliJ plugin displays a window tool view which, by default, is at the bottom of the screen.

The window tool can be accessed at: View -> Tool windows -> JFrog 

![Alt text](docs/enable_tool_window.png?raw=true "Enable tool window")

#### Scanning and viewing the results
JFrog Xray automatically performs a scan whenever there is a change in dependencies in the project.

To manually invoke a scan, click ```Refresh``` button in JFrog Plugin tool window.

![Alt text](docs/tool_window.png?raw=true "Scan results window")

#### Filtering Xray Scan Results
There are two ways to filter the scan results:
1. **Issue severity:** Only display issues with the specified severity.
2. **Component license:** Only display components with the specified licenses.


![Alt text](docs/filter_issues.png?raw=true "Issues filter")
![Alt text](docs/filter_licenses.png?raw=true "Licenses filter")
# Building and Testing the Sources
To buid the plugin sources, please follow these steps:
1. Clone the code from git.
2. CD to the *xray* directory located under the *jfrog-idea-plugin* directory.
3. Build and install the *xray-client-java* dependency in your local maven repository, by running the following gradle command:
```
gradle clean install
```
4. If you'd like run the *xray-client-java* integration tests, follow these steps:
* Make sure your Xray instance is up and running.
* Set the *CLIENTTESTS_XRAY_URL*, *CLIENTTESTS_XRAY_USERNAME* and *CLIENTTESTS_XRAY_PASSWORD* environment variables with your Xray URL, username and password.
* Run the following command:
```
gradle test
```
5. From IntelliJ, open the plugin project, by selecting *jfrog-idea-plugin/plugin/build.gradle* file.
6. Build the sources and launch the plugin by the following these steps:
* From the *Gradle Projects* window, expand *Idea --> Tasks -->  IntelliJ*
* Run the *buildPlugin* task.
* Run the *runIdea* task.
