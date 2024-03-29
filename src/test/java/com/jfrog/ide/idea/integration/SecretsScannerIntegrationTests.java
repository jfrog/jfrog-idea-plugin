package com.jfrog.ide.idea.integration;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.SecretsScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;

public class SecretsScannerIntegrationTests extends BaseIntegrationTest {

    private SecretsScannerExecutor scanner;
    private final static String TEST_PROJECT_PREFIX = "secrets/testProjects/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new SecretsScannerExecutor(Logger.getInstance());
    }

    public void testSecretsScanner() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("exposedSecrets");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));
        ProgressIndicator indicator = mock(ProgressIndicator.class);

        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled, indicator);
        assertEquals(8, results.size());
        // Expect specific indications
        JFrogSecurityWarning secretIndication = results.get(0);
        assertEquals(0, secretIndication.getLineStart());
        assertEquals(0, secretIndication.getLineEnd());
        assertEquals(6, secretIndication.getColStart());
        assertEquals(118, secretIndication.getColEnd());
        assertTrue(secretIndication.getFilePath().endsWith("applicable_base64.js"));
        assertEquals(SourceCodeScanType.SECRETS, secretIndication.getReporter());
        assertTrue(StringUtils.isNotBlank(secretIndication.getScannerSearchTarget()));
        assertTrue(StringUtils.isNotBlank(secretIndication.getReason()));
    }

    public void testSecretsScannerNoSecrets() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("dummy");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));
        ProgressIndicator indicator = mock(ProgressIndicator.class);
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled, indicator);
        assertEquals(0, results.size());
    }

    @Override
    protected String createTempProjectDir(String projectName) throws IOException {
        return super.createTempProjectDir(TEST_PROJECT_PREFIX + projectName);
    }
}
