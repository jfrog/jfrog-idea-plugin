package com.jfrog.ide.idea.scan;

import com.jfrog.ide.idea.inspections.JfrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;

import java.io.IOException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class Eos extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-codebase";
    private static final String SCANNER_BINARY_NAME = "Eos";

    public Eos() {
        super(SCAN_TYPE, SCANNER_BINARY_NAME);
    }

    @Override
    List<String> getSupportedLanguages() {
        return List.of("python");
    }

    public List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, List.of("analyze", "config"),true);
    }

}
