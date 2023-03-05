package com.jfrog.ide.idea.integration;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.ui.configuration.ConnectionRetriesSpinner;
import com.jfrog.ide.idea.ui.configuration.ConnectionTimeoutSpinner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.DEFAULT_EXCLUSIONS;

public abstract class BaseIntegrationTest extends HeavyPlatformTestCase {
    public static final String ENV_PLATFORM_URL = "JFROG_IDE_PLATFORM_URL";
    public static final String ENV_ACCESS_TOKEN = "JFROG_IDE_ACCESS_TOKEN";
    private final static Path TEST_PROJECT_PATH = new File("src/test/resources/applicability/testProjects").toPath();

    protected ServerConfigImpl serverConfig;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverConfig = createServerConfigFromEnv();
        GlobalSettings.getInstance().updateConfig(serverConfig);
    }

    private ServerConfigImpl createServerConfigFromEnv() {
        String platformUrl = addSlashIfNeeded(readParam(ENV_PLATFORM_URL));
        String token = readParam(ENV_ACCESS_TOKEN);
        return createServerConfig(platformUrl, token);
    }

    private ServerConfigImpl createServerConfig(String platformUrl, String token) {
        return new ServerConfigImpl.Builder()
                .setUrl(platformUrl)
                .setXrayUrl(platformUrl + "xray")
                .setArtifactoryUrl(platformUrl + "artifactory")
                .setAccessToken(token)
                .setConnectionRetries(ConnectionRetriesSpinner.RANGE.initial)
                .setConnectionTimeout(ConnectionTimeoutSpinner.RANGE.initial)
                .setExcludedPaths(DEFAULT_EXCLUSIONS)
                .build();
    }

    private String readParam(String paramName) {
        String paramValue = System.getenv(paramName);
        if (StringUtils.isBlank(paramValue)) {
            failSetup();
        }
        return paramValue;
    }

    private String addSlashIfNeeded(String paramValue) {
        return StringUtils.appendIfMissing(paramValue, "/");
    }

    private void failSetup() {
        String message = String.format("Failed to load JFrog platform credentials.\n Looking for Environment variables %s and %s", ENV_PLATFORM_URL, ENV_ACCESS_TOKEN);
        Assert.fail(message);
    }

    protected String createTempProjectDir(String projectName) throws IOException {
        String tempProjectDir = getTempDir().createVirtualDir(projectName).toNioPath().toString();
        FileUtils.copyDirectory(TEST_PROJECT_PATH.resolve(projectName).toFile(), new File(tempProjectDir));
        return tempProjectDir;
    }
}
