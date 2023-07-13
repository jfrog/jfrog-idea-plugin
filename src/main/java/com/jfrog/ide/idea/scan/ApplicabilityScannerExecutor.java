package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class ApplicabilityScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("ca");
    private static final List<PackageManagerType> SUPPORTED_PACKAGE_TYPES = List.of(PackageManagerType.PYPI, PackageManagerType.NPM, PackageManagerType.YARN, PackageManagerType.GRADLE, PackageManagerType.MAVEN);


    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, "", true);
    }

    public ApplicabilityScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SourceCodeScanType.CONTEXTUAL, binaryDownloadUrl, log, serverConfig, useJFrogReleases);
        supportedPackageTypes = SUPPORTED_PACKAGE_TYPES;
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled);
    }

    @Override
    Feature getScannerFeatureName() {
        return Feature.CONTEXTUAL_ANALYSIS;
    }
}
