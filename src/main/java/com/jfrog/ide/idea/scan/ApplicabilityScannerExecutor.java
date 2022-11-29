package com.jfrog.ide.idea.scan;

import com.jfrog.ide.idea.inspections.JfrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;

import java.io.IOException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-applicability";
    private static final String SCANNER_BINARY_NAME = "applicability_scanner";

    public ApplicabilityScannerExecutor() {
        super(SCAN_TYPE, SCANNER_BINARY_NAME);
    }

    public List<JfrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, List.of("scan"),false);
    }

}
