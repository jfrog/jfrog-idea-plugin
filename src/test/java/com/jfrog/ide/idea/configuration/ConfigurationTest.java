package com.jfrog.ide.idea.configuration;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import com.intellij.credentialStore.Credentials;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.jfrog.ide.idea.ui.configuration.Utils;

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
    private static final int CONNECTION_TIMEOUT = 70;
    private static final int CONNECTION_RETRIES = 5;
    private static final String PASSWORD = "prince";
    private static final String USERNAME = "diana";

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
    }

    /**
     * Test set server config in the GlobalSettings with migration from file to PasswordSafe.
     */
    public void testSetServerConfigCredentialsMigration() {
        // Create overriding server config
        GlobalSettings globalSettings = new GlobalSettings();
        ServerConfigImpl overrideServerConfig = createServerConfig(true, true);
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

        // Check credential were migrated to PasswordSafe
        Credentials credentials = actualServerConfig.getCredentialsFromPasswordSafe();
        assertNotNull(credentials);
        assertEquals(USERNAME, credentials.getUserName());
        assertEquals(PASSWORD, credentials.getPasswordAsString());
    }

    /**
     * Test set server config from environment variables.
     */
    public void testSetServerConfigFromEnv() throws Exception {
        SystemLambda.withEnvironmentVariable(PLATFORM_URL_ENV, "https://tython.jfrog.io")
                .and(XRAY_URL_ENV, "https://tython.jfrog.io/xray")
                .and(ARTIFACTORY_URL_ENV, "https://tython.jfrog.io/artifactory")
                .and(USERNAME_ENV, "leia")
                .and(PASSWORD_ENV, "princess").execute(() -> {
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
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
        });
    }

    /**
     * Test set server config from JFROG_IDE_URL legacy environment variable.
     */
    @SuppressWarnings("deprecation")
    public void testSetServerConfigFromLegacyEnv() throws Exception {
        SystemLambda.withEnvironmentVariable(LEGACY_XRAY_URL_ENV, "https://tython.jfrog.io/xray")
                .and(USERNAME_ENV, "leia")
                .and(PASSWORD_ENV, "princess").execute(() -> {
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
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
        });
    }

    /**
     * Test set server config from JFROG_IDE_URL legacy environment variable.
     * This time, the URL provided could not be resolved to the platform URL.
     */
    @SuppressWarnings("deprecation")
    public void testSetServerConfigFromXrayLegacyEnv() throws Exception {
        SystemLambda.withEnvironmentVariable(LEGACY_XRAY_URL_ENV, "https://tython-xray.jfrog.io")
                .and(PLATFORM_URL_ENV, "")
                .and(ARTIFACTORY_URL_ENV, "")
                .and(XRAY_URL_ENV, "")
                .and(USERNAME_ENV, "leia")
                .and(PASSWORD_ENV, "princess").execute(() -> {
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
            assertEquals(CONNECTION_RETRIES, actualServerConfig.getConnectionRetries());
            assertEquals(CONNECTION_TIMEOUT, actualServerConfig.getConnectionTimeout());
            assertEquals(EXCLUDED_PATHS, actualServerConfig.getExcludedPaths());
        });
    }

    /**
     * Test migration from XrayServerConfig to ServerConfig.
     */
    @SuppressWarnings("deprecation")
    public void testMigrateXrayConfigFromFile() {
        // Create overriding server config
        GlobalSettings globalSettings = new GlobalSettings();
        XrayServerConfigImpl xrayServerConfig = createLegacyServerConfig();
        xrayServerConfig.setUsername(USERNAME);
        xrayServerConfig.setPassword(PASSWORD);
        globalSettings.setXrayConfig(xrayServerConfig);

        // Check that the xrayServerConfig was migrated to serverConfig
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

        // Make sure credentials migrated PasswordSafe
        Credentials credentials = actualServerConfig.getCredentialsFromPasswordSafe();
        assertEquals(USERNAME, credentials.getUserName());
        assertEquals(PASSWORD, credentials.getPasswordAsString());
    }

    @SuppressWarnings("deprecation")
    public void testMigrateXrayConfigFromPasswordSafe() {
        // Create overriding server config
        GlobalSettings globalSettings = new GlobalSettings();
        XrayServerConfigImpl xrayServerConfig = createLegacyServerConfig();
        Credentials credentials = new Credentials(USERNAME, PASSWORD);
        Utils.storeCredentialsInPasswordSafe(XRAY_SETTINGS_CREDENTIALS_KEY, xrayServerConfig.getUrl(), credentials);
        globalSettings.setXrayConfig(xrayServerConfig);

        // Check that the xrayServerConfig was migrated to serverConfig
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

        // Make sure credentials migrated in PasswordSafe
        credentials = Utils.retrieveCredentialsFromPasswordSafe(XRAY_SETTINGS_CREDENTIALS_KEY, xrayServerConfig.getUrl());
        assertNull(credentials);
        credentials = actualServerConfig.getCredentialsFromPasswordSafe();
        assertEquals(USERNAME, credentials.getUserName());
        assertEquals(PASSWORD, credentials.getPasswordAsString());
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
