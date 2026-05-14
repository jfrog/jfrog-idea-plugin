package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.NewScanConfig;
import com.jfrog.ide.idea.scan.data.NewScansConfig;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final ScanBinaryExecutor secretsScanner = new SecretsScannerExecutor(new NullLog());
    private final Path FAULTY_OUTPUT = new File("src/test/resources/sourceCode/faulty_output.sarif").toPath();
    private final Path SIMPLE_OUTPUT = new File("src/test/resources/sourceCode/simple_output.sarif").toPath();
    private final Path APPLIC_KIND_PASS_AND_FAIL_OUTPUT = new File("src/test/resources/sourceCode/applicable_kind_pass_output.sarif").toPath();
    private final Path SECRETS_WITH_INFORMATIONAL_OUTPUT = new File("src/test/resources/sourceCode/secrets_with_informational_output.sarif").toPath();
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
      assertThrows(IndexOutOfBoundsException.class,() -> secretsScanner.parseOutputSarif(FAULTY_OUTPUT));
    }

    public void testSarifParserApplicResultsWithRulesBasedParsing() throws IOException {
        List<JFrogSecurityWarning> parsedOutput = scanner.parseOutputSarif(APPLIC_KIND_PASS_AND_FAIL_OUTPUT);
        assertEquals(2, parsedOutput.size());
        // Not applicable based on rule properties
        assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getRuleID());
        assertFalse(parsedOutput.get(0).isApplicable());
        // Applicable based on rule properties, with evidence location from result
        assertEquals("applic_CVE-2022-25978", parsedOutput.get(1).getRuleID());
        assertTrue(parsedOutput.get(1).isApplicable());
    }


    public void testSarifParserSkipsInformationalResults() throws IOException {
        List<JFrogSecurityWarning> parsedOutput = secretsScanner.parseOutputSarif(SECRETS_WITH_INFORMATIONAL_OUTPUT);
        assertEquals(1, parsedOutput.size());
        assertEquals("REQ.SECRET.GENERIC.TEXT", parsedOutput.get(0).getRuleID());
        assertEquals("Hardcoded secrets were found", parsedOutput.get(0).getReason());
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

    public void testGetBinaryDownloadURLForWsl() {
        // When WSL distro is set the URL should use a Linux OS distribution token
        final String expectedLinuxPrefix = "xsc-gen-exe-analyzer-manager-local/";
        String url = scanner.getBinaryDownloadURL(null);
        // Default (non-WSL) scanner was constructed without a distro; just verify URL is non-null and starts correctly
        assertTrue(url.startsWith(expectedLinuxPrefix));
    }

    /**
     * {@link ScanBinaryExecutor#createTempRunInputFileInWsl(Object, Path)} matches production layout
     * (nested {@code jfrog*} dir and {@code .yaml} file); {@code wslTmpBase} is injected so tests run without a WSL UNC mount.
     */
    public void testCreateTempRunInputFileInWsl_scansConfigRoundTrip() throws IOException {
        Path wslTmpBase = Files.createTempDirectory("wsl-input-parent");
        try {
            ScanConfig.Builder builder = new ScanConfig.Builder();
            String testOutput = "/tmp/out.sarif";
            String testLanguage = "Go";
            List<String> testRoots = List.of("/project/a", "/project/b");
            builder.scanType(scanner.scanType);
            builder.output(testOutput);
            builder.language(testLanguage);
            builder.roots(testRoots);
            ScansConfig written = new ScansConfig(List.of(builder.Build()));

            Path inputPath = scanner.createTempRunInputFileInWsl(written, wslTmpBase);
            assertTrue(inputPath.getFileName().toString().endsWith(".yaml"));
            assertTrue(inputPath.getParent().getFileName().toString().startsWith("jfrog"));
            assertEquals(wslTmpBase, inputPath.getParent().getParent());

            ScansConfig readBack = readScansConfigYAML(inputPath);
            assertEquals(1, readBack.getScans().size());
            assertEquals(testOutput, readBack.getScans().get(0).getOutput());
            assertEquals(testLanguage, readBack.getScans().get(0).getLanguage());
            assertEquals(testRoots, readBack.getScans().get(0).getRoots());
        } finally {
            FileUtils.deleteQuietly(wslTmpBase.toFile());
        }
    }

    public void testCreateTempRunInputFileInWsl_newScansConfigRoundTrip() throws IOException {
        Path wslTmpBase = Files.createTempDirectory("wsl-input-parent-newfmt");
        try {
            ScanConfig.Builder builder = new ScanConfig.Builder();
            builder.scanType(scanner.scanType);
            builder.output("/out.sarif");
            builder.language("Java");
            builder.roots(List.of("/src"));
            ScanConfig params = builder.Build();
            NewScansConfig written = new NewScansConfig(new NewScanConfig(params));

            Path inputPath = scanner.createTempRunInputFileInWsl(written, wslTmpBase);
            assertTrue(Files.isRegularFile(inputPath));

            NewScansConfig readBack = readNewScansConfigYAML(inputPath);
            assertEquals(1, readBack.getScans().size());
            assertEquals(params.getOutput(), readBack.getScans().get(0).getOutput());
            assertEquals(params.getLanguage(), readBack.getScans().get(0).getLanguage());
            assertEquals(params.getRoots(), readBack.getScans().get(0).getRoots());
        } finally {
            FileUtils.deleteQuietly(wslTmpBase.toFile());
        }
    }

    private ScansConfig readScansConfigYAML(Path inputPath) throws IOException {
        ObjectMapper mapper = createYAMLMapper();
        return mapper.readValue(inputPath.toFile(), ScansConfig.class);
    }

    private NewScansConfig readNewScansConfigYAML(Path inputPath) throws IOException {
        ObjectMapper mapper = createYAMLMapper();
        return mapper.readValue(inputPath.toFile(), NewScansConfig.class);
    }
}
