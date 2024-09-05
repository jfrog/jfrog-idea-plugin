package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;
import static org.junit.Assert.assertThrows;

/**
 * @author tala
 **/
public class ScanBinaryExecutorTest extends TestCase {
    private final ScanBinaryExecutor scanner = new ApplicabilityScannerExecutor(new NullLog());
    private final Path FAULTY_OUTPUT = new File("src/test/resources/sourceCode/faulty_output.sarif").toPath();
    private final Path SIMPLE_OUTPUT = new File("src/test/resources/sourceCode/simple_output.sarif").toPath();
    private final Path APPLIC_KIND_PASS_AND_FAIL_OUTPUT = new File("src/test/resources/sourceCode/applicable_kind_pass_output.sarif").toPath();
    public void testInputBuilder() throws IOException {
        ScanConfig.Builder inputFileBuilder = new ScanConfig.Builder();
        Path inputPath = null;
        String testOutput = "file/location\\out.sarif";
        String testLanguage = "Go";
        List<String> testRoots = List.of("a", "b", "c");

        inputFileBuilder.scanType(scanner.scanType);
        inputFileBuilder.output(testOutput);
        inputFileBuilder.language(testLanguage);
        inputFileBuilder.roots(testRoots);
        try {
            inputPath = scanner.createTempRunInputFile(new ScansConfig(List.of(inputFileBuilder.Build())));
            ScansConfig inputFile = readScansConfigYAML(inputPath);
            assertNotNull(inputFile);
            assertEquals(1, inputFile.getScans().size());
            assertEquals(testOutput, inputFile.getScans().get(0).getOutput());
            assertEquals(testLanguage, inputFile.getScans().get(0).getLanguage());
            assertEquals(testRoots, inputFile.getScans().get(0).getRoots());
        } finally {
            if (inputPath != null) {
                FileUtils.deleteQuietly(inputPath.toFile());
            }
        }
    }

    public void testSarifParser() throws IOException {
        List<JFrogSecurityWarning> parsedOutput = scanner.parseOutputSarif(SIMPLE_OUTPUT);
        String expectedPath = Paths.get("/examples/applic-demo/index.js").toString();
        assertEquals(2, parsedOutput.size());
        assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getRuleID());
        assertEquals("CVE-2022-25978", parsedOutput.get(1).getRuleID());
        assertEquals(expectedPath, parsedOutput.get(0).getFilePath());
        assertEquals(expectedPath, parsedOutput.get(1).getFilePath());
        assertEquals("The vulnerable function protobufjs.load is called", parsedOutput.get(0).getReason());
        assertEquals("The vulnerable function protobufjs.parse is called.", parsedOutput.get(1).getReason());
        assertEquals(19, parsedOutput.get(0).getLineStart());
        assertEquals(17, parsedOutput.get(1).getLineStart());
        assertEquals(19, parsedOutput.get(0).getLineEnd());
        assertEquals(21, parsedOutput.get(1).getLineEnd());
        assertEquals(0, parsedOutput.get(0).getColStart());
        assertEquals(0, parsedOutput.get(1).getColStart());
        assertEquals(17, parsedOutput.get(0).getColEnd());
        assertEquals(73, parsedOutput.get(1).getColEnd());
    }

    public void testSarifParserWithMissingRole() throws IndexOutOfBoundsException {
      assertThrows(IndexOutOfBoundsException.class,() -> scanner.parseOutputSarif(FAULTY_OUTPUT));
    }

    public void testSarifParserApplicResultsWithKindPassAndFail() throws IOException {
        List<JFrogSecurityWarning> parsedOutput = scanner.parseOutputSarif(APPLIC_KIND_PASS_AND_FAIL_OUTPUT);
        assertEquals(6, parsedOutput.size());
        //Not Applicable with kind pass
        assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getRuleID());
        assertFalse(parsedOutput.get(0).isApplicable());
        //Applicable with kind pass
        assertEquals("applic_CVE-2022-25978", parsedOutput.get(1).getRuleID());
        assertTrue(parsedOutput.get(1).isApplicable());
        //Not applicable with kind pass and no properties
        assertEquals("applic_CVE-2021-25878", parsedOutput.get(2).getRuleID());
        assertFalse(parsedOutput.get(2).isApplicable());
        //Applicable with kind fail
        assertEquals("applic_CVE-2022-29019", parsedOutput.get(3).getRuleID());
        assertTrue(parsedOutput.get(3).isApplicable());
        //Not applicable as its not_covered
        assertEquals("applic_CVE-2022-29004", parsedOutput.get(4).getRuleID());
        assertFalse(parsedOutput.get(4).isApplicable());
        //Not applicable as its undetermined
        assertEquals("applic_CVE-2022-29014", parsedOutput.get(5).getRuleID());
        assertFalse(parsedOutput.get(5).isApplicable());
    }


    public void testGetBinaryDownloadURL() {
        final String externalRepoName = "test-releases-repo";
        final String expectedExternalRepoUrl = "test-releases-repo/artifactory/xsc-gen-exe-analyzer-manager-local/";
        final String expectedNoExternalRepoUrl = "xsc-gen-exe-analyzer-manager-local/";

        String actualNoExternalRepoUrl = scanner.getBinaryDownloadURL(null);
        assertTrue(actualNoExternalRepoUrl.startsWith(expectedNoExternalRepoUrl));
        String actualExternalRepoUrl = scanner.getBinaryDownloadURL(externalRepoName);
        assertTrue(actualExternalRepoUrl.startsWith(expectedExternalRepoUrl));
    }

    private ScansConfig readScansConfigYAML(Path inputPath) throws IOException {
        ObjectMapper mapper = createYAMLMapper();
        return mapper.readValue(inputPath.toFile(), ScansConfig.class);
    }
}
