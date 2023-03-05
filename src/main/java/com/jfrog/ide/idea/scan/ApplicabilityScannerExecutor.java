package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.Output;
import com.jfrog.ide.idea.scan.data.Rule;
import com.jfrog.ide.idea.scan.data.Run;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-applicability";
    private static final String SCANNER_BINARY_NAME = "analyzerManager";
    private static final List<String> SCANNER_ARGS = List.of("ca");
    private static final String DEFAULT_BINARY_DOWNLOAD_URL = "xsc-gen-exe-analyzer-manager-local/v1/[RELEASE]";
    private static final String DOWNLOAD_SCANNER_NAME = "analyzerManager.zip";
    private final String BINARY_DOWNLOAD_URL;


    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, DEFAULT_BINARY_DOWNLOAD_URL, true);
    }

    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SCAN_TYPE, SCANNER_BINARY_NAME, DOWNLOAD_SCANNER_NAME, log, serverConfig, useJFrogReleases);
        supportedLanguages = List.of("python", "js");
        BINARY_DOWNLOAD_URL = defaultIfEmpty(binaryDownloadUrl, DEFAULT_BINARY_DOWNLOAD_URL);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS);
    }

    @Override
    String getBinaryDownloadURL() {
        return String.format("%s/%s/%s", BINARY_DOWNLOAD_URL, getOsDistribution(), DOWNLOAD_SCANNER_NAME);
    }

    @Override
    Feature getScannerFeatureName() {
        return Feature.ContextualAnalysis;
    }

    @Override
    protected List<JFrogSecurityWarning> parseOutputSarif(Path outputFile) throws IOException {
        List<JFrogSecurityWarning> results = super.parseOutputSarif(outputFile);
        Output output = getOutputObj(outputFile);
        Optional<Run> run = output.getRuns().stream().findFirst();
        if (run.isPresent()) {
            List<Rule> scanners = run.get().getTool().getDriver().getRules();
            // Adds the scanner search target data
            for (JFrogSecurityWarning warning : results) {
                String ScannerSearchTarget = scanners.stream().filter(scanner -> scanner.getId().equals(warning.getName())).findFirst().map(scanner -> scanner.getFullDescription().getText()).orElse("");
                warning.setScannerSearchTarget(ScannerSearchTarget);
            }
        }
        return results;
    }

}
