package com.jfrog.ide.idea.integration;

import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.ApplicabilityScannerExecutor;
import com.jfrog.ide.idea.scan.data.ScanConfig;

import java.io.IOException;
import java.util.List;

public class ApplicabilityScannerIntegrationTests extends BaseIntegrationTest {
    private ApplicabilityScannerExecutor scanner;
    private final static String TEST_PROJECT_PREFIX = "sourceCode/testProjects/";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new ApplicabilityScannerExecutor(Logger.getInstance());
    }

    public void testApplicabilityScannerJsProjectNotApplicable() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("npm");
        ScanConfig.Builder input = new ScanConfig.Builder().roots(List.of(testProjectRoot)).cves(List.of("CVE-2021-3918", "CVE-2021-3807"));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(2, results.size());
        // Expect all issues to be not applicable to this test project
        assertFalse(results.stream().anyMatch(JFrogSecurityWarning::isApplicable));
    }

    public void testApplicabilityScannerJsProject() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("npm");
        ScanConfig.Builder input = new ScanConfig.Builder().roots(List.of(testProjectRoot)).cves(List.of("CVE-2022-25878"));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(2, results.size());
        // Expect all issues to be applicable.
        assertTrue(results.stream().allMatch(JFrogSecurityWarning::isApplicable));
        // Expect specific indications
        assertEquals("protobuf.parse(p)", results.get(0).getLineSnippet());
        assertEquals(20, results.get(0).getLineStart());
        assertEquals(20, results.get(0).getLineEnd());
        assertEquals(0, results.get(0).getColStart());
        assertEquals(17, results.get(0).getColEnd());
        assertTrue(results.get(0).getFilePath().endsWith("index.js"));
        assertEquals(SourceCodeScanType.CONTEXTUAL, results.get(0).getReporter());

    }

    public void testApplicabilityScannerPythonProjectNotApplicable() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("python");
        ScanConfig.Builder input = new ScanConfig.Builder().roots(List.of(testProjectRoot)).cves(List.of("CVE-2021-3918", "CVE-2019-15605"));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(2, results.size());
        // Expect all issues to be not applicable to this test project
        assertFalse(results.stream().anyMatch(JFrogSecurityWarning::isApplicable));
    }

    public void testApplicabilityScannerPythonProject() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("python");
        ScanConfig.Builder input = new ScanConfig.Builder().roots(List.of(testProjectRoot)).cves(List.of("CVE-2019-20907"));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(1, results.size());
        // Expect specific indications
        assertTrue(results.get(0).isApplicable());
        assertEquals("tarfile.open(name)", results.get(0).getLineSnippet());
        assertEquals(16, results.get(0).getLineStart());
        assertEquals(16, results.get(0).getLineEnd());
        assertEquals(6, results.get(0).getColStart());
        assertEquals(24, results.get(0).getColEnd());
        assertTrue(results.get(0).getFilePath().endsWith("main.py"));
        assertEquals(SourceCodeScanType.CONTEXTUAL, results.get(0).getReporter());
    }

    public void testApplicabilityScannerJavaProject() throws IOException, InterruptedException {
        String testProjectRoot = createTempProjectDir("maven");
        ScanConfig.Builder input = new ScanConfig.Builder().roots(List.of(testProjectRoot)).cves(List.of("CVE-2013-7285"));
        List<JFrogSecurityWarning> results = scanner.execute(input, this::dummyCheckCanceled);
        assertEquals(2, results.size());
        // Expect specific indications
        assertTrue(results.get(0).isApplicable());
        assertEquals("xstream.fromXML(payload)", results.get(0).getLineSnippet());
        assertEquals(56, results.get(0).getLineStart());
        assertEquals(56, results.get(0).getLineEnd());
        assertEquals(26, results.get(0).getColStart());
        assertEquals(50, results.get(0).getColEnd());
        assertTrue(results.get(0).getFilePath().endsWith("VulnerableComponentsLesson.java"));
        assertEquals(SourceCodeScanType.CONTEXTUAL, results.get(0).getReporter());
    }

    @Override
    protected String createTempProjectDir(String projectName) throws IOException {
        return super.createTempProjectDir(TEST_PROJECT_PREFIX + projectName);
    }
}
