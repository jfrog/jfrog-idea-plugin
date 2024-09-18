package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SastIssueNode;
import com.jfrog.ide.common.nodes.subentities.SourceCodeScanType;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Tal Arian
 */
public class SastScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("zd");
    // This variable is used to indicate that this scanner supports only the new config (input) format.
    // In the near future, when all scanners will use only the new input file structure this variable as well
    // as the ScanConfig and ScanConfigs classes can be safely removed.
    private static final boolean RUN_WITH_NEW_CONFIG_FILE = true;
    private static final List<PackageManagerType> SUPPORTED_PACKAGE_TYPES = List.of(PackageManagerType.PYPI, PackageManagerType.NPM, PackageManagerType.YARN, PackageManagerType.GRADLE, PackageManagerType.MAVEN);

    public SastScannerExecutor(Log log) {
        super(SourceCodeScanType.SAST, log);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled, RUN_WITH_NEW_CONFIG_FILE, indicator);
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

            FileIssueNode issueNode = new SastIssueNode(warning.getReason(),
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    warning.getScannerSearchTarget(), warning.getLineSnippet(), warning.getCodeFlows(), warning.getSeverity(), warning.getRuleID());
            fileNode.addIssue(issueNode);
        }
        return new ArrayList<>(results.values());
    }

    @Override
    public Feature getScannerFeatureName() {
        // TODO: change to SAST feature when Xray entitlement service supports it.
        return Feature.CONTEXTUAL_ANALYSIS;
    }

    @Override
    protected boolean isPackageTypeSupported(PackageManagerType packageType) {
        return packageType != null && SUPPORTED_PACKAGE_TYPES.contains(packageType);
    }
}
