package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.components.ApplicableIssueNode;
import com.jfrog.ide.common.components.FileTreeNode;
import com.jfrog.ide.common.components.VulnerabilityNode;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.idea.configuration.GlobalSettings;
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
import java.util.regex.Matcher;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.*;
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
     * @param indicator - the progress indicator.
     * @param cves      - white list of CVEs to scan.
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
                indicator.setText("Running applicability scan");
                indicator.setFraction(0.25);
                List<JFrogSecurityWarning> applicabilityResults = applicability.execute(new ScanConfig.Builder().roots(List.of(getProjectBasePath(project).toString())).cves(cves).skippedFolders(getSkippedFoldersPatterns()));
                scanResults.addAll(applicabilityResults);
            }
            if (eos.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Running Eos scan");
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

    /**
     * Splits the users' configured ExcludedPaths glob pattern to a list
     * of simplified patterns by avoiding the use of "{}".
     *
     * @return a list of equivalent patterns without the use of "{}"
     */
    private List<String> getSkippedFoldersPatterns() {
        String excludePattern = GlobalSettings.getInstance().getServerConfig().getExcludedPaths();
        List<String> skippedFoldersPatterns = new ArrayList<>();
        Matcher matcher = EXCLUSIONS_REGEX_PATTERN.matcher(excludePattern);
        if (!matcher.find()) {
            return List.of(excludePattern);
        }
        String[] dirsNames = matcher.group(1).split(",");
        for (String dirName : dirsNames) {
            skippedFoldersPatterns.add(EXCLUSIONS_PREFIX + dirName.strip() + EXCLUSIONS_SUFFIX);
        }
        return skippedFoldersPatterns;
    }

    public List<FileTreeNode> getResults(Map<String, List<VulnerabilityNode>> issuesMap) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : scanResults) {
            FileTreeNode fileNode = results.get(warning.getFilePath());
            if (fileNode == null && warning.isApplicable()) {
                fileNode = new FileTreeNode(warning.getFilePath());
                results.put(warning.getFilePath(), fileNode);
            }
            String cve = StringUtils.removeStart(warning.getName(), "applic_");
            List<VulnerabilityNode> issues = issuesMap.get(cve);
            if (issues != null) {
                if (warning.isApplicable()) {
                    ApplicableIssueNode applicableIssue = new ApplicableIssueNode(
                            cve, warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                            warning.getFilePath(), warning.getReason(), warning.getLineSnippet(), warning.getScannerSearchTarget(),
                            issues.get(0));
                    fileNode.addDependency(applicableIssue);
                    for (VulnerabilityNode issue : issues) {
                        issue.AddApplicableIssues(applicableIssue);
                    }
                } else {
                    // Mark non applicable issues
                    for (VulnerabilityNode issue : issues) {
                        issue.setApplicableIssues(new ArrayList<>());
                    }
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    public List<JFrogSecurityWarning> getScanResults() {
        return scanResults != null ? new ArrayList<>(scanResults) : new ArrayList<>();
    }
}
