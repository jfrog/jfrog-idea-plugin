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
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public abstract class BaseIntegrationTest extends HeavyPlatformTestCase {
    public static final String ENV_PLATFORM_URL = "JFROG_IDE_PLATFORM_URL";
    public static final String ENV_ACCESS_TOKEN = "JFROG_IDE_ACCESS_TOKEN";
    protected String binaryDownloadUrl;
    protected boolean useReleases;
    protected ServerConfigImpl serverConfig;
    private final static Path TEST_PROJECT_PATH = new File("src/test/resources/").toPath();
    private static final String ENV_BINARY_DOWNLOAD_URL = "JFROG_IDE_ANALYZER_MANAGER_DOWNLOAD_URL";
    private static final String ENV_DOWNLOAD_FROM_JFROG_RELEASES = "JFROG_IDE_DOWNLOAD_FROM_JFROG_RELEASES";
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverConfig = createServerConfigFromEnv();
        if (serverConfig != null) {
            GlobalSettings.getInstance().updateConfig(serverConfig);
        }
        // Try to use the loaded config from JFrog CLI.
        serverConfig = GlobalSettings.getInstance().getServerConfig();
        // If not configured, fail the setup.
        if (!serverConfig.isXrayConfigured()) {
            failSetup();
        }
        binaryDownloadUrl = System.getenv(ENV_BINARY_DOWNLOAD_URL);
        useReleases = Boolean.parseBoolean(defaultIfEmpty(System.getenv(ENV_DOWNLOAD_FROM_JFROG_RELEASES), "true"));
    }

    private ServerConfigImpl createServerConfigFromEnv() {
        String platformUrl = addSlashIfNeeded(System.getenv(ENV_PLATFORM_URL));
        String token = System.getenv(ENV_ACCESS_TOKEN);
        if (StringUtils.isEmpty(platformUrl) || StringUtils.isEmpty(ENV_ACCESS_TOKEN)) {
            return null;
        }
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


    private String addSlashIfNeeded(String paramValue) {
        return StringUtils.appendIfMissing(paramValue, "/");
    }

    private void failSetup() {
        String message = String.format("Failed to load JFrog platform credentials.\n Looking for Environment variables %s and %s\n Or installed JFrog CLI with configured server.", ENV_PLATFORM_URL, ENV_ACCESS_TOKEN);
        Assert.fail(message);
    }

    protected String createTempProjectDir(String projectName) throws IOException {
        String tempProjectDir = getTempDir().createVirtualDir().toNioPath().toString();
        FileUtils.copyDirectory(TEST_PROJECT_PATH.resolve(projectName).toFile(), new File(tempProjectDir));
        return tempProjectDir;
    }
}
