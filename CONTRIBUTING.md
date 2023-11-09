# Guidelines

- If the existing tests do not already cover your changes, please add tests.

## Building and Testing the Sources

To build the plugin sources, please follow these steps:

1. Clone the code from git.
2. Build and create the JFrog IDEA Plugin zip file by running the following gradle command:

    ```bash
    ./gradlew clean build
    ```

After the build finishes, you'll find the zip file in the *build/distributions* directory, located under the
*jfrog-idea-plugin* directory.
The zip file can be loaded into IntelliJ.

## Additional Tests Suits

1. In order to run the Python tests suite, run the following gradle command:

    ```bash
    ./gradlew clean pythonTests
    ```

2. In order to run the integration tests:
    - Make sure you have JFrog platform Instance with JAS enabled.
    - If you are using JFrog CLI, just make sure the current configured server is the one you want to use.
      Alternatively, you can set JFROG_IDE_PLATFORM_URL and JFROG_IDE_ACCESS_TOKEN environment variables with your JFrog
      Platform URL and access token, respectively.
    - Set the JFROG_IDE_TEST_EXTERNAL_RESOURCES_REPO environment variable to the name of a remote repository in your
      Artifactory instance that proxies https://releases.jfrog.io/.

   Run the following command:

     ```bash
     ./gradlew integrationTests
     ```

## Debugging the Plugin Code

To build and run the plugin following your code changes, follow these steps:

1. From IntelliJ, open the plugin project, by selecting *jfrog-idea-plugin/build.gradle* file.
2. Build the sources and launch the plugin by the following these steps:

- From the *Gradle Projects* window, expand *Idea --> Tasks -->  IntelliJ*
- Debug the *runIdea* task.
