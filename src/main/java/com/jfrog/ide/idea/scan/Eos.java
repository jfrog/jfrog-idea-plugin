package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class Eos extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-codebase";
    private static final String DOWNLOAD_SCANNER_NAME = "analyzerManager.zip";

    private static final String SCANNER_BINARY_NAME = "Eos";
    private static final List<String> SCANNER_ARGS = List.of("analyze", "config");

    public Eos(Log log, ServerConfig server) {
        super(SCAN_TYPE, SCANNER_BINARY_NAME, DOWNLOAD_SCANNER_NAME, log, server);
        supportedLanguages = List.of("python");
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException, URISyntaxException {
        return super.execute(inputFileBuilder, SCANNER_ARGS);
    }

    @Override
    String getBinaryDownloadURL() {
        return null;
    }

    @Override
    Feature getScannerFeatureName() {
        return null;
    }

}
