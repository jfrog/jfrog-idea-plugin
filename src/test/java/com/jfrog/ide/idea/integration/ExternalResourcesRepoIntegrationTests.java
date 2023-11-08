package com.jfrog.ide.idea.integration;

import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ScanBinaryExecutor;
import com.jfrog.ide.idea.scan.SecretsScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ExternalResourcesRepoIntegrationTests extends BaseIntegrationTest {
    private static final String TEST_PROJECT_PREFIX = "secrets/testProjects/";
    private static final String ENV_EXTERNAL_RESOURCES_REPO = "JFROG_IDE_TEST_EXTERNAL_RESOURCES_REPO";

    private SecretsScannerExecutor scanner;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new SecretsScannerExecutor(Logger.getInstance());
    }

    public void testDownloadScannersFromExternalRepo() throws IOException, InterruptedException {
        String externalResourcesRepo = System.getenv(ENV_EXTERNAL_RESOURCES_REPO);
        if (StringUtils.isEmpty(externalResourcesRepo)) {
            fail("The " + ENV_EXTERNAL_RESOURCES_REPO + " environment variable must be set to run this test");
        }

        // Save the current ServerConfig and restore it at the end
        ServerConfigImpl originalServerConfig = GlobalSettings.getInstance().getServerConfig();

        ServerConfigImpl serverConfig = Mockito.spy(GlobalSettings.getInstance().getServerConfig());
        Mockito.when(serverConfig.getExternalResourcesRepo()).thenReturn(externalResourcesRepo);
        GlobalSettings.getInstance().setServerConfig(serverConfig);
        deleteScannersDir();
        String testProjectRoot = createTempProjectDir("exposedSecrets");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(8, results.size());

        // Restore the original ServerConfig in GlobalSettings
        GlobalSettings.getInstance().setServerConfig(originalServerConfig);
    }

    public void testDownloadScannersFromExternalRepoNotExist() throws IOException, InterruptedException {
        // Save the current ServerConfig and restore it at the end
        ServerConfigImpl originalServerConfig = GlobalSettings.getInstance().getServerConfig();

        ServerConfigImpl serverConfig = Mockito.spy(GlobalSettings.getInstance().getServerConfig());
        Mockito.when(serverConfig.getExternalResourcesRepo()).thenReturn("repo-that-does-not-exist");
        GlobalSettings.getInstance().setServerConfig(serverConfig);
        deleteScannersDir();
        GlobalSettings.getInstance().reloadMissingConfiguration();
        String testProjectRoot = createTempProjectDir("exposedSecrets");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));
        try {
            List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);

            // An exception should've been thrown. If not, the test failed.
            fail();
        } catch (FileNotFoundException e) {
        } catch (Throwable e) {
            // If an exception other than FileNotFoundException was thrown, then the test failed.
            fail();
        } finally {
            // Restore the original ServerConfig in GlobalSettings
            GlobalSettings.getInstance().setServerConfig(originalServerConfig);
        }
    }

    @Override
    protected String createTempProjectDir(String projectName) throws IOException {
        return super.createTempProjectDir(TEST_PROJECT_PREFIX + projectName);
    }

    private void deleteScannersDir() throws IOException {
        if (Files.isDirectory(ScanBinaryExecutor.BINARIES_DIR)) {
            FileUtils.deleteDirectory(ScanBinaryExecutor.BINARIES_DIR.toFile());
        }
    }
}
