package com.jfrog.ide.idea.scan;

import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;

import java.io.IOException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class Eos extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-codebase";
    private static final String SCANNER_BINARY_NAME = "Eos";
    private static final List<String> SCANNER_ARGS = List.of("analyze", "config");

    public Eos() {
        super(SCAN_TYPE, SCANNER_BINARY_NAME);
        supportedLanguages = List.of("python");
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS);
    }

}
