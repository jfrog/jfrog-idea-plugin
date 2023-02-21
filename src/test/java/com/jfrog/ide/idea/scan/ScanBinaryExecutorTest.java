package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.ide.idea.ServerConfigStub;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.gradle.internal.impldep.org.testng.annotations.Test;
import org.jfrog.build.api.util.NullLog;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createYAMLMapper;

/**
 * @author tala
 **/
public class ScanBinaryExecutorTest extends TestCase {
    private final ScanBinaryExecutor scanner = new ApplicabilityScannerExecutor(new NullLog(), new ServerConfigStub());
    private final Path SIMPLE_OUTPUT = new File("src/test/resources/applicability/simple_output.sarif").toPath();
    private final Path NOT_APPLIC_OUTPUT = new File("src/test/resources/applicability/not_applic_output.sarif").toPath();


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
        assertEquals(2, parsedOutput.size());
        assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getName());
        assertEquals("CVE-2022-25978", parsedOutput.get(1).getName());
        assertEquals("examples/applic-demo/../applic-demo/index.js", parsedOutput.get(0).getFilePath());
        assertEquals("examples/applic-demo/../applic-demo/index.js", parsedOutput.get(1).getFilePath());
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

    public void testSarifParserNotApplicResults() throws IOException {
        List<JFrogSecurityWarning> parsedOutput = scanner.parseOutputSarif(NOT_APPLIC_OUTPUT);
        assertEquals(4, parsedOutput.size());
        // 2 known applicable results (code evidence returned)
        assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getName());
        assertTrue(parsedOutput.get(0).isApplicable());
        assertEquals("CVE-2022-25978", parsedOutput.get(1).getName());
        assertTrue(parsedOutput.get(1).isApplicable());
        // 2 known no-applicable results (have a scanner but no code evidence returned)
        assertEquals("applic_CVE-2021-25878", parsedOutput.get(2).getName());
        assertFalse(parsedOutput.get(2).isApplicable());
        assertEquals("applic_CVE-2022-29019", parsedOutput.get(3).getName());
        assertFalse(parsedOutput.get(3).isApplicable());

    }

    private ScansConfig readScansConfigYAML(Path inputPath) throws IOException {
        ObjectMapper mapper = createYAMLMapper();
        return mapper.readValue(inputPath.toFile(), ScansConfig.class);
    }
}
