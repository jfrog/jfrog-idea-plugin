package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.ApplicableIssue;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    private final Eos eos = new Eos();
    private final ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor();

    private List<JFrogSecurityWarning> scanResults;

    protected Project project;
    protected String codeBaseLanguage;

    public SourceCodeScannerManager(Project project, String codeBaseLanguage) {
        this.project = project;
        this.codeBaseLanguage = codeBaseLanguage.toLowerCase();
    }

    /**
     * Source code scan and update components.
     *
     * @param indicator - The progress indicator
     * @param cves      - white list of cves to scan
     */
    public void scanAndUpdate(ProgressIndicator indicator, List<String> cves) {
        if (project.isDisposed()) {
            return;
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            return;
        }
        scanResults = new ArrayList<>();
        try {
            if (applicability.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Applicability Scan");
                indicator.setFraction(0.25);
                List<JFrogSecurityWarning> applicabilityResults = applicability.execute(new ScanConfig.Builder().roots(List.of(getProjectBasePath(project).toString())).cves(cves));
                scanResults.addAll(applicabilityResults);
            }
            if (eos.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Eos Scan");
                indicator.setFraction(0.5);
                List<JFrogSecurityWarning> eosResults = eos.execute(new ScanConfig.Builder().language(codeBaseLanguage).roots(List.of(getProjectBasePath(project).toString())));
                scanResults.addAll(eosResults);
            }
        } catch (IOException | InterruptedException |
                 NullPointerException e) {
            logError(Logger.getInstance(), "Failed to scan source code", e, true);
        } finally {
            scanInProgress.set(false);
            indicator.setFraction(1);
        }
    }

    public List<FileTreeNode> getResults(Map<String, List<Issue>> issuesMap) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : scanResults) {
            FileTreeNode fileNode = results.get(warning.getFilePath());
            if (fileNode == null) {
                fileNode = new FileTreeNode(warning.getFilePath());
                results.put(warning.getFilePath(), fileNode);
            }
            String cve = StringUtils.removeStart(warning.getName(), "applic_");
            List<Issue> issues = issuesMap.get(cve);
            if (issues != null) {
                ApplicableIssue applicableIssue = new ApplicableIssue(cve, warning.getLineStart(), warning.getColStart(), warning.getFilePath(), warning.getReason(), warning.getLineSnippet(), issues.get(0));
                fileNode.addDependency(applicableIssue);
                for (Issue issue : issues) {
                    issue.AddApplicableIssues(applicableIssue);
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    public List<JFrogSecurityWarning> getScanResults() {
        return scanResults != null ? new ArrayList<>(scanResults) : new ArrayList<>();
    }
}
