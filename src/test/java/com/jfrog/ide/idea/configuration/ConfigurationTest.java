package com.jfrog.ide.idea.configuration;

import com.intellij.credentialStore.Credentials;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.EnvironmentUtil;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.jfrog.ide.idea.configuration.ServerConfigImpl.*;

/**
 * @author yahavi
 **/
public class ConfigurationTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String JFROG_SETTINGS_CREDENTIALS_KEY = "com.jfrog.ideaTest";
    private static final String XRAY_SETTINGS_CREDENTIALS_KEY = "com.jfrog.xray.ideaTest";
    private static final String ARTIFACTORY_URL = "https://steve.jfrog.io/artifactory";
    private static final String XRAY_URL = "https://steve.jfrog.io/xray";
    private static final String PLATFORM_URL = "https://steve.jfrog.io";
    private static final String EXCLUDED_PATHS = "**/*{ares}*";
    private static final String JFROG_PROJECT = "ideaTest";
    private static final int CONNECTION_TIMEOUT = 70;
    private static final int CONNECTION_RETRIES = 5;
    private static final String PASSWORD = "prince";
    private static final String USERNAME = "diana";
    private static final String WATCH = "heimdall";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cleanUp();
    }

    @Override
    protected void tearDown() throws Exception {
        cleanUp();
        super.tearDown();
    }

    /**
     * Test credentials storage.
     */
    public void testStoreCredentials() {
        ServerConfigImpl serverConfig = createServerConfig(true, true);

        // Add credentials
        serverConfig.addCredentialsToPasswordSafe();

        // Check credentials
        Credentials credentials = serverConfig.getCredentialsFromPasswordSafe();
        assertNotNull(credentials);
        assertEquals(USERNAME, credentials.getUserName());
        assertEquals(PASSWORD, credentials.getPasswordAsString());

        // Remove credentials
        serverConfig.removeCredentialsFromPasswordSafe();
        assertNull(serverConfig.getCredentialsFromPasswordSafe());
    }

    /**
     * Test set server config in the GlobalSettings.
     */
    public void testSetServerConfig() {
        // Create overriding server config
        GlobalSettings globalSettings = new GlobalSettings();
        ServerConfigImpl overrideServerConfig = createServerConfig(true, true);

        // Save credentials in the PasswordSafe and delete credentials from the overriding server.
        // We do this to simulate GlobalSettings load from file.
        overrideServerConfig.addCredentialsToPasswordSafe();
        overrideServerConfig.setUsername("");
        overrideServerConfig.setPassword("");
        globalSettings.setServerConfig(overrideServerConfig);

        // Check that the server in the global settings was overridden.
        ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
        assertFalse(actualServerConfig.isConnectionDetailsFromEnv());
        assertEquals(PLATFORM_URL, actualServerConfig.getUrl());
        assertEquals(XRAY_URL, actualServerConfig.getXrayUrl());
        assertEquals(ARTIFACTORY_URL, actualServerConfig.getArtifactoryUrl());
        assertEquals(USERNAME, actualServerConfig.getUsername());
        assertEquals(PASSWORD, actualServerConfig.getPassword());
        assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
        assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
        assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
        assertEquals(JFROG_PROJECT, actualServerConfig.getProject());
        assertEquals(WATCH, actualServerConfig.getWatches());
    }

    /**
     * Test policy types in the GlobalSettings.
     */
    public void testPolicyType() {
        GlobalSettings globalSettings = new GlobalSettings();

        // Check "vulnerabilities" policy type
        ServerConfigImpl serverConfig = new ServerConfigImpl.Builder().setPolicyType(PolicyType.VULNERABILITIES).build();
        globalSettings.setServerConfig(serverConfig);
        ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
        assertEquals(PolicyType.VULNERABILITIES, actualServerConfig.getPolicyType());

        // Check "project" policy type
        serverConfig = new ServerConfigImpl.Builder().setPolicyType(PolicyType.PROJECT).build();
        globalSettings.setServerConfig(serverConfig);
        actualServerConfig = globalSettings.getServerConfig();
        assertEquals(PolicyType.PROJECT, actualServerConfig.getPolicyType());

        // Check "watch" policy type
        serverConfig = new ServerConfigImpl.Builder().setPolicyType(PolicyType.WATCHES).build();
        globalSettings.setServerConfig(serverConfig);
        actualServerConfig = globalSettings.getServerConfig();
        assertEquals(PolicyType.WATCHES, actualServerConfig.getPolicyType());
    }

    /**
     * Test set server config from environment variables.
     */
    public void testSetServerConfigFromEnv() {
        try (MockedStatic<EnvironmentUtil> mockController = Mockito.mockStatic(EnvironmentUtil.class)) {
            mockController.when(() -> EnvironmentUtil.getValue(PLATFORM_URL_ENV)).thenReturn("https://tython.jfrog.io");
            mockController.when(() -> EnvironmentUtil.getValue(XRAY_URL_ENV)).thenReturn("https://tython.jfrog.io/xray");
            mockController.when(() -> EnvironmentUtil.getValue(ARTIFACTORY_URL_ENV)).thenReturn("https://tython.jfrog.io/artifactory");
            mockController.when(() -> EnvironmentUtil.getValue(USERNAME_ENV)).thenReturn("leia");
            mockController.when(() -> EnvironmentUtil.getValue(PASSWORD_ENV)).thenReturn("princess");
            mockController.when(() -> EnvironmentUtil.getValue(PROJECT_ENV)).thenReturn("x");

            // Create overriding server config
            GlobalSettings globalSettings = new GlobalSettings();
            ServerConfigImpl overrideServerConfig = createServerConfig(false, false);

            // Make sure that readConnectionDetailsFromEnv indeed returned true when the connection details environment variables set
            overrideServerConfig.setConnectionDetailsFromEnv(overrideServerConfig.readConnectionDetailsFromEnv());

            // Check that the server in the global settings was overridden by the environment variables
            globalSettings.setServerConfig(overrideServerConfig);
            ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
            assertTrue(actualServerConfig.readConnectionDetailsFromEnv());
            assertEquals("https://tython.jfrog.io", actualServerConfig.getUrl());
            assertEquals("https://tython.jfrog.io/xray", actualServerConfig.getXrayUrl());
            assertEquals("https://tython.jfrog.io/artifactory", actualServerConfig.getArtifactoryUrl());
            assertEquals("leia", actualServerConfig.getUsername());
            assertEquals("princess", actualServerConfig.getPassword());
            assertEquals("x", actualServerConfig.getProject());
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
            assertEquals(JFROG_PROJECT, actualServerConfig.getProject());
            assertEquals(WATCH, actualServerConfig.getWatches());
        }
    }

    /**
     * Test set server config from JFROG_IDE_URL legacy environment variable.
     */
    @SuppressWarnings("deprecation")
    public void testSetServerConfigFromLegacyEnv() {
        try (MockedStatic<EnvironmentUtil> mockController = Mockito.mockStatic(EnvironmentUtil.class)) {
            mockController.when(() -> EnvironmentUtil.getValue(LEGACY_XRAY_URL_ENV)).thenReturn("https://tython.jfrog.io/xray");
            mockController.when(() -> EnvironmentUtil.getValue(USERNAME_ENV)).thenReturn("leia");
            mockController.when(() -> EnvironmentUtil.getValue(PASSWORD_ENV)).thenReturn("princess");
            // Create overriding server config
            GlobalSettings globalSettings = new GlobalSettings();
            ServerConfigImpl overrideServerConfig = createServerConfig(false, false);

            // Make sure that readConnectionDetailsFromEnv indeed returned true when the connection details environment variables set
            overrideServerConfig.setConnectionDetailsFromEnv(overrideServerConfig.readConnectionDetailsFromEnv());

            // Check that the server in the global settings was overridden by the environment variables
            globalSettings.setServerConfig(overrideServerConfig);
            ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
            assertTrue(actualServerConfig.readConnectionDetailsFromEnv());
            assertEquals("https://tython.jfrog.io", actualServerConfig.getUrl());
            assertEquals("https://tython.jfrog.io/xray", actualServerConfig.getXrayUrl());
            assertEquals("https://tython.jfrog.io/artifactory", actualServerConfig.getArtifactoryUrl());
            assertEquals("leia", actualServerConfig.getUsername());
            assertEquals("princess", actualServerConfig.getPassword());
            assertEquals("", actualServerConfig.getProject());
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
            assertEquals(JFROG_PROJECT, actualServerConfig.getProject());
            assertEquals(WATCH, actualServerConfig.getWatches());
        }
    }

    /**
     * Test set server config from JFROG_IDE_URL legacy environment variable.
     * This time, the URL provided could not be resolved to the platform URL.
     */
    @SuppressWarnings("deprecation")
    public void testSetServerConfigFromXrayLegacyEnv() {
        try (MockedStatic<EnvironmentUtil> mockController = Mockito.mockStatic(EnvironmentUtil.class)) {
            mockController.when(() -> EnvironmentUtil.getValue(LEGACY_XRAY_URL_ENV)).thenReturn("https://tython-xray.jfrog.io");
            mockController.when(() -> EnvironmentUtil.getValue(PLATFORM_URL_ENV)).thenReturn("");
            mockController.when(() -> EnvironmentUtil.getValue(ARTIFACTORY_URL_ENV)).thenReturn("");
            mockController.when(() -> EnvironmentUtil.getValue(USERNAME_ENV)).thenReturn("leia");
            mockController.when(() -> EnvironmentUtil.getValue(PASSWORD_ENV)).thenReturn("princess");

            // Create overriding server config
            GlobalSettings globalSettings = new GlobalSettings();
            ServerConfigImpl overrideServerConfig = createServerConfig(false, false);

            // Make sure that readConnectionDetailsFromEnv indeed returned true when the connection details environment variables set
            overrideServerConfig.setConnectionDetailsFromEnv(overrideServerConfig.readConnectionDetailsFromEnv());

            // Check that the server in the global settings was overridden by the environment variables
            globalSettings.setServerConfig(overrideServerConfig);
            ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
            assertTrue(actualServerConfig.readConnectionDetailsFromEnv());
            assertEquals("", actualServerConfig.getUrl());
            assertEquals("https://tython-xray.jfrog.io", actualServerConfig.getXrayUrl());
            assertEquals("", actualServerConfig.getArtifactoryUrl());
            assertEquals("leia", actualServerConfig.getUsername());
            assertEquals("princess", actualServerConfig.getPassword());
            assertEquals("", actualServerConfig.getProject());
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
            assertEquals(JFROG_PROJECT, actualServerConfig.getProject());
            assertEquals(WATCH, actualServerConfig.getWatches());
        }
    }

    /**
     * Test migration from XrayServerConfig to ServerConfig.
     */
    @SuppressWarnings("deprecation")
    public void testMigrateXrayConfigFromFile() {
        // Create overriding server config
        GlobalSettings globalSettings = new GlobalSettings();
        XrayServerConfigImpl xrayServerConfig = createLegacyServerConfig();
        globalSettings.setXrayConfig(xrayServerConfig);

        // Check that the xrayServerConfig was migrated to serverConfig
        ServerConfigImpl actualServerConfig = globalSettings.getServerConfig();
        assertFalse(actualServerConfig.isConnectionDetailsFromEnv());
        assertEquals(PLATFORM_URL, actualServerConfig.getUrl());
        assertEquals(XRAY_URL, actualServerConfig.getXrayUrl());
        assertEquals(ARTIFACTORY_URL, actualServerConfig.getArtifactoryUrl());
        assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
        assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
        assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
        assertEquals(JFROG_PROJECT, actualServerConfig.getProject());
        assertEquals(WATCH, actualServerConfig.getWatches());
    }

    /**
     * Create server config for the tests.
     *
     * @param xrayUrl        - True if should set Xray URL
     * @param artifactoryUrl - True if should set Artifactory URL
     * @return server config
     */
    ServerConfigImpl createServerConfig(boolean xrayUrl, boolean artifactoryUrl) {
        return new ServerConfigImpl.Builder()
                .setJFrogSettingsCredentialsKey(JFROG_SETTINGS_CREDENTIALS_KEY)
                .setUrl(PLATFORM_URL)
                .setXrayUrl(xrayUrl ? XRAY_URL : "")
                .setArtifactoryUrl(artifactoryUrl ? ARTIFACTORY_URL : "")
                .setUsername(USERNAME)
                .setPassword(PASSWORD)
                .setConnectionRetries(CONNECTION_RETRIES)
                .setConnectionTimeout(CONNECTION_TIMEOUT)
                .setExcludedPaths(EXCLUDED_PATHS)
                .setProject(JFROG_PROJECT)
                .setWatches(WATCH)
                .build();
    }

    /**
     * Create server config for the tests.
     *
     * @return server config
     */
    @SuppressWarnings("deprecation")
    private XrayServerConfigImpl createLegacyServerConfig() {
        return (XrayServerConfigImpl) new XrayServerConfigImpl.Builder()
                .setXraySettingsCredentialsKey(XRAY_SETTINGS_CREDENTIALS_KEY)
                .setUrl(XRAY_URL)
                .setConnectionRetries(CONNECTION_RETRIES)
                .setConnectionTimeout(CONNECTION_TIMEOUT)
                .setExcludedPaths(EXCLUDED_PATHS)
                .build();
    }

    /**
     * Clean up PasswordSafe.
     */
    @SuppressWarnings("deprecation")
    private void cleanUp() {
        createServerConfig(true, true).removeCredentialsFromPasswordSafe();
        createLegacyServerConfig().removeLegacyCredentialsFromPasswordSafe();
    }
}
