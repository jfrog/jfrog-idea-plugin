package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.List;

/**
 * @author Tal Arian
 */
public class SecretsScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("sec");

    public SecretsScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, null, true);
    }

    public SecretsScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SourceCodeScanType.SECRETS, binaryDownloadUrl, log, serverConfig, useJFrogReleases);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled);
    }

    @Override
    public Feature getScannerFeatureName() {
        return Feature.SecretsScanner;
    }

    @Override
    protected boolean isPackageTypeSupported(PackageManagerType packageType) {
        return true;
    }
}
