package com.jfrog.ide.idea.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.data.ScansConfig;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author tala
 **/
public class ScanBinaryExecutorTest extends TestCase {
    private ScanBinaryExecutor scanner;
    private final Path TEST_DEMO_OUTPUT = new File("src/test/resources/sarif/demo_output.sarif").toPath();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new ApplicabilityScannerExecutor();

    }

    public void testInputBuilder() {
        var inputFileBuilder = new ScanConfig.Builder();
        Path inputPath = null;
        var testOutput = "file/location\\out.sarif";
        var testLanguage = "Go";
        var testRoots = List.of("a", "b", "c");

        inputFileBuilder.scanType(scanner.SCAN_TYPE);
        inputFileBuilder.output(testOutput);
        inputFileBuilder.language(testLanguage);
        inputFileBuilder.roots(testRoots);
        try {
            inputPath = scanner.createTempRunInputFile(new ScansConfig(List.of(inputFileBuilder.Build())));
            var inputFile = readScansConfigYAML(inputPath);
            assertNotNull(inputFile);
            assertEquals(1, inputFile.getScans().size());
            assertEquals(testOutput, inputFile.getScans().get(0).getOutput());
            assertEquals(testLanguage, inputFile.getScans().get(0).getLanguage());
            assertEquals(testRoots, inputFile.getScans().get(0).getRoots());

        } catch (Exception e) {
            assertNull(e);
        } finally {
            if (inputPath != null) {
                FileUtils.deleteQuietly(inputPath.toFile());
            }
        }
    }

    public void testSarifParser() {
        try {
            var parsedOutput = scanner.parseOutputSarif(TEST_DEMO_OUTPUT);
            assertEquals(2, parsedOutput.size());
            assertEquals("applic_CVE-2022-25878", parsedOutput.get(0).getName());
            assertEquals("CVE-2022-25978", parsedOutput.get(1).getName());
            assertEquals("examples/applic-demo/../applic-demo/index.js", parsedOutput.get(0).getFilePath());
            assertEquals("file://examples/applic-demo/../applic-demo/index.js", parsedOutput.get(1).getFilePath());
            assertEquals("The vulnerable function protobufjs.load is called", parsedOutput.get(0).getReason());
            assertEquals("The vulnerable function protobufjs.parse is called.", parsedOutput.get(1).getReason());
            assertEquals(20, parsedOutput.get(0).getLineStart());
            assertEquals(18, parsedOutput.get(1).getLineStart());
            assertEquals(20, parsedOutput.get(0).getLineEnd());
            assertEquals(22, parsedOutput.get(1).getLineEnd());
            assertEquals(0, parsedOutput.get(0).getColStart());
            assertEquals(0, parsedOutput.get(1).getColStart());
            assertEquals(17, parsedOutput.get(0).getColEnd());
            assertEquals(73, parsedOutput.get(1).getColEnd());

        } catch (IOException e) {
            assertNull(e);
        }

    }

    private ScansConfig readScansConfigYAML(Path inputPath) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(inputPath.toFile(), ScansConfig.class);
    }


}
