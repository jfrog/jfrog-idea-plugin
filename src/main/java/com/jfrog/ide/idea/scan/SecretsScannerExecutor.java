package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.FileIssueNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import com.jfrog.ide.idea.scan.utils.SourceScanType;
import com.jfrog.xray.client.services.entitlements.Feature;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jfrog.ide.idea.scan.utils.SourceScanType.toSourceCodeScanType;

/**
 * @author Tal Arian
 */
public class SecretsScannerExecutor extends ScanBinaryExecutor {
    private static final List<String> SCANNER_ARGS = List.of("sec");
    private static final String ISSUE_TITLE = "Potential Secret";

    public SecretsScannerExecutor(Log log) {
        super(SourceScanType.SECRETS, log);
    }

    public List<JFrogSecurityWarning> execute(ScanConfig.Builder inputFileBuilder, Runnable checkCanceled, ProgressIndicator indicator) throws IOException, InterruptedException {
        return super.execute(inputFileBuilder, SCANNER_ARGS, checkCanceled, indicator);
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

            FileIssueNode issueNode = new FileIssueNode(ISSUE_TITLE,
                    warning.getFilePath(), warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                    warning.getScannerSearchTarget(), warning.getLineSnippet(), toSourceCodeScanType(warning.getReporter()), warning.getSeverity(), warning.getRuleID());
            fileNode.addIssue(issueNode);

        }
        return new ArrayList<>(results.values());
    }

    @Override
    public Feature getScannerFeatureName() {
        return Feature.SECRETS;
    }

    @Override
    protected boolean isPackageTypeSupported(PackageManagerType packageType) {
        return true;
    }
}
