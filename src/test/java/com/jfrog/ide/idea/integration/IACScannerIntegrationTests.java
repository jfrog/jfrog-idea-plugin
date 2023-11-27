package com.jfrog.ide.idea.integration;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.IACScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.mock;

public class IACScannerIntegrationTests extends BaseIntegrationTest {

    private IACScannerExecutor scanner;
    private final static String TEST_PROJECT_PREFIX = "iac/testProjects/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new IACScannerExecutor(Logger.getInstance());
    }

    public void testIACScanner() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("exposedIac");
        ScanConfig.Builder input = new ScanConfig.Builder()
                .roots(List.of(testProjectRoot));
        ProgressIndicator indicator = mock(ProgressIndicator.class);
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled, indicator);
        assertEquals(11, results.size());
        // Expect specific indications
        JFrogSecurityWarning iacIndication = results.get(0);
        assertEquals(0, iacIndication.getLineStart());
        assertEquals(11, iacIndication.getLineEnd());
        assertEquals(0, iacIndication.getColStart());
        assertEquals(1, iacIndication.getColEnd());
        assertTrue(iacIndication.getFilePath().endsWith("req_sw_terraform_aws_alb_https_only.tf"));
        assertEquals(SourceCodeScanType.IAC, iacIndication.getReporter());
        assertTrue(StringUtils.isNotBlank(iacIndication.getScannerSearchTarget()));
        assertTrue(StringUtils.isNotBlank(iacIndication.getReason()));
    }

    @Override
    protected String createTempProjectDir(String projectName) throws IOException {
        return super.createTempProjectDir(TEST_PROJECT_PREFIX + projectName);
    }
}
