package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.configuration.ServerConfig;
import com.jfrog.ide.common.nodes.SastIssueNode;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Tal Arian
 */
public class SastScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("zd");
    private static final boolean RUN_WITH_NEW_CONFIG_FILE = true;
    private static final List<PackageManagerType> SUPPORTED_PACKAGE_TYPES = List.of(PackageManagerType.PYPI, PackageManagerType.NPM, PackageManagerType.YARN, PackageManagerType.GRADLE, PackageManagerType.MAVEN);

    public SastScannerExecutor(Log log, ServerConfig serverConfig) {
        this(log, serverConfig, null, true);
    }

    public SastScannerExecutor(Log log, ServerConfig serverConfig, String binaryDownloadUrl, boolean useJFrogReleases) {
        super(SourceCodeScanType.SAST, binaryDownloadUrl, log, serverConfig, useJFrogReleases);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled, RUN_WITH_NEW_CONFIG_FILE);
    }

    @Override
    List<FileTreeNode> createSpecificFileIssueNodes(List<JFrogSecurityWarning> warnings) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : warnings) {
            // Create FileTreeNodes for files with found issues
            FileTreeNode fileNode = results.get(warning.getFilePath());
            if (fileNode == null) {
                fileNode = new FileTreeNode(warning.getFilePath());
                results.put(warning.getFilePath(), fileNode);
            }

            FileIssueNode issueNode = new SastIssueNode(warning.getRuleID(),
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    warning.getScannerSearchTarget(), warning.getLineSnippet(), warning.getCodeFlows(), warning.getSeverity(), warning.getRuleID());
            fileNode.addIssue(issueNode);
        }
        return new ArrayList<>(results.values());
    }

    @Override
    public Feature getScannerFeatureName() {
        // TODO: change to SASST feature when Xray entitlement service support it.
        return Feature.CONTEXTUAL_ANALYSIS;
    }

    @Override
    protected boolean isPackageTypeSupported(PackageManagerType packageType) {
        return packageType != null && SUPPORTED_PACKAGE_TYPES.contains(packageType);
    }
}
