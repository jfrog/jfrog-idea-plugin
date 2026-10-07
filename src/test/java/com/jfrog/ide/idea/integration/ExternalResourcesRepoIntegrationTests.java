package com.jfrog.ide.idea.integration;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.configuration.ServerConfigImpl;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ScanBinaryExecutor;
import com.jfrog.ide.idea.scan.SecretsScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.mockito.Mockito;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static com.jfrog.ide.common.utils.ArtifactoryConnectionUtils.createArtifactoryManagerBuilder;
import static org.mockito.Mockito.mock;

public class ExternalResourcesRepoIntegrationTests extends BaseIntegrationTest {
    private static final String TEST_PROJECT_PREFIX = "secrets/testProjects/";
    private static final String RELEASES_REMOTE_CONFIG = "{\"rclass\":\"remote\",\"packageType\":\"generic\",\"url\":\"https://releases.jfrog.io\"}";

    private SecretsScannerExecutor scanner;
    private String externalResourcesRepo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new SecretsScannerExecutor(Logger.getInstance());
        String repository = "ide-plugin-releases-" + UUID.randomUUID();
        try (ArtifactoryManager artifactoryManager = createArtifactoryManagerBuilder(serverConfig, Logger.getInstance()).build()) {
            artifactoryManager.createRepository(repository, RELEASES_REMOTE_CONFIG);
        }
        externalResourcesRepo = repository;
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (externalResourcesRepo != null) {
                try (ArtifactoryManager artifactoryManager = createArtifactoryManagerBuilder(serverConfig, Logger.getInstance()).build()) {
                    artifactoryManager.deleteRepository(externalResourcesRepo);
                }
            }
        } finally {
            super.tearDown();
        }
    }

    public void testDownloadScannersFromExternalRepo() throws IOException, InterruptedException {
        // Save the current ServerConfig and restore it at the end
        ServerConfigImpl originalServerConfig = GlobalSettings.getInstance().getServerConfig();

        ServerConfigImpl serverConfig = Mockito.spy(GlobalSettings.getInstance().getServerConfig());
        Mockito.when(serverConfig.getExternalResourcesRepo()).thenReturn(externalResourcesRepo);
        GlobalSettings.getInstance().setServerConfig(serverConfig);
        deleteScannersDir();
        String testProjectRoot = createTempProjectDir("exposedSecrets");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));

        ProgressIndicator indicator = mock(ProgressIndicator.class);
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled, indicator);
        assertEquals(7, results.size());

        // Restore the original ServerConfig in GlobalSettings
        GlobalSettings.getInstance().setServerConfig(originalServerConfig);
    }

    public void testDownloadScannersFromExternalRepoNotExist() throws IOException {
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
        ProgressIndicator indicator = mock(ProgressIndicator.class);
        assertThrows(FileNotFoundException.class, () -> scanner.execute(input, this::dummyCheckCanceled, indicator));
        // Restore the original ServerConfig in GlobalSettings
        GlobalSettings.getInstance().setServerConfig(originalServerConfig);
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
