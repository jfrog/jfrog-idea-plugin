package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.*;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final String SCAN_TYPE = "analyze-applicability";
    private static final List<String> SCANNER_ARGS = List.of("ca");
    private static final List<PackageType> SUPPORTED_PACKAGE_TYPES = List.of(PackageType.PYPI, PackageType.NPM, PackageType.YARN);


    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, "", true);
    }

    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SCAN_TYPE, binaryDownloadUrl, log, serverConfig, useJFrogReleases);
        supportedPackageTypes = SUPPORTED_PACKAGE_TYPES;
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled);
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
