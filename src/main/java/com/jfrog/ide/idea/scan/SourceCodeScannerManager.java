package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.nodes.ApplicableIssueNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.inspections.JFrogSecurityWarning;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.apache.commons.lang.StringUtils;

import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.ui.configuration.ConfigVerificationUtils.*;
import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final Eos eos = new Eos();
    private final ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor();

    protected Project project;
    protected String codeBaseLanguage;

    public SourceCodeScannerManager(Project project, String codeBaseLanguage) {
        this.project = project;
        this.codeBaseLanguage = codeBaseLanguage.toLowerCase();
    }

    /**
     * Source code scan and update components.
     *
     * @param indicator      the progress indicator.
     * @param depScanResults collection of DependencyNodes.
     * @return A list of FileTreeNodes having the source code issues as their children.
     */
    public List<FileTreeNode> scanAndUpdate(ProgressIndicator indicator, Collection<DependencyNode> depScanResults) {
        if (project.isDisposed()) {
            return Collections.emptyList();
        }
        // Prevent multiple simultaneous scans
        if (!scanInProgress.compareAndSet(false, true)) {
            return Collections.emptyList();
        }
        List<JFrogSecurityWarning> scanResults = new ArrayList<>();
        Map<String, List<VulnerabilityNode>> issuesMap = mapIssuesByCve(depScanResults);
        try {
            if (applicability.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Running applicability scan");
                indicator.setFraction(0.25);
                List<JFrogSecurityWarning> applicabilityResults = applicability.execute(new ScanConfig.Builder().roots(List.of(getProjectBasePath(project).toString())).cves(List.copyOf(issuesMap.keySet())).skippedFolders(getSkippedFoldersPatterns()));
                scanResults.addAll(applicabilityResults);
            }
            if (eos.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Running Eos scan");
                indicator.setFraction(0.5);
                List<JFrogSecurityWarning> eosResults = eos.execute(new ScanConfig.Builder().language(codeBaseLanguage).roots(List.of(getProjectBasePath(project).toString())));
                scanResults.addAll(eosResults);
            }
        } catch (IOException | InterruptedException | URISyntaxException |
                 NullPointerException e) {
            logError(Logger.getInstance(), "Failed to scan source code", e, true);
        } finally {
            scanInProgress.set(false);
            indicator.setFraction(1);
        }
        return createAndUpdateNodes(scanResults, issuesMap);
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
        if (StringUtils.isNotBlank(excludePattern)) {
            Matcher matcher = EXCLUSIONS_REGEX_PATTERN.matcher(excludePattern);
            if (!matcher.find()) {
                return List.of(excludePattern);
            }
            String[] dirsNames = matcher.group(1).split(",");
            for (String dirName : dirsNames) {
                skippedFoldersPatterns.add(EXCLUSIONS_PREFIX + dirName.strip() + EXCLUSIONS_SUFFIX);
            }
        }
        return skippedFoldersPatterns;
    }

    /**
     * Create {@link FileTreeNode}s with applicability issues and update applicability issues in {@link VulnerabilityNode}s.
     *
     * @param scanResults a list of source code scan results.
     * @param issuesMap   a map of {@link VulnerabilityNode}s mapped by their CVEs.
     * @return a list of new {@link FileTreeNode}s containing source code issues.
     */
    private List<FileTreeNode> createAndUpdateNodes(List<JFrogSecurityWarning> scanResults, Map<String, List<VulnerabilityNode>> issuesMap) {
        HashMap<String, FileTreeNode> results = new HashMap<>();
        for (JFrogSecurityWarning warning : scanResults) {
            // Update all VulnerabilityNodes that have the warning's CVE
            String cve = StringUtils.removeStart(warning.getName(), "applic_");
            List<VulnerabilityNode> issues = issuesMap.get(cve);
            if (issues != null) {
                if (warning.isApplicable()) {
                    // Create FileTreeNodes for files with applicable issues
                    FileTreeNode fileNode = results.get(warning.getFilePath());
                    if (fileNode == null && warning.isApplicable()) {
                        fileNode = new FileTreeNode(warning.getFilePath());
                        results.put(warning.getFilePath(), fileNode);
                    }

                    ApplicableIssueNode applicableIssue = new ApplicableIssueNode(
                            cve, warning.getLineStart(), warning.getColStart(), warning.getLineEnd(), warning.getColEnd(),
                            warning.getFilePath(), warning.getReason(), warning.getLineSnippet(), warning.getScannerSearchTarget(),
                            issues.get(0));
                    //noinspection DataFlowIssue
                    fileNode.addIssue(applicableIssue);
                    for (VulnerabilityNode issue : issues) {
                        issue.addApplicableIssue(applicableIssue);
                    }
                } else {
                    // Mark non-applicable vulnerabilities by setting an empty list of applicability issues
                    for (VulnerabilityNode issue : issues) {
                        issue.setApplicableIssues(new ArrayList<>());
                    }
                }
            }
        }
        return new ArrayList<>(results.values());
    }

    /**
     * Maps all the issues (vulnerabilities and security violations) by their CVE IDs.
     * Issues without a CVE ID are ignored.
     *
     * @param depScanResults - collection of DependencyNodes.
     * @return a map of CVE IDs to lists of issues with them.
     */
    private Map<String, List<VulnerabilityNode>> mapIssuesByCve(Collection<DependencyNode> depScanResults) {
        Map<String, List<VulnerabilityNode>> issues = new HashMap<>();
        for (DependencyNode dep : depScanResults) {
            Enumeration<TreeNode> treeNodeEnumeration = dep.children();
            while (treeNodeEnumeration.hasMoreElements()) {
                TreeNode node = treeNodeEnumeration.nextElement();
                if (!(node instanceof VulnerabilityNode)) {
                    continue;
                }
                VulnerabilityNode vulnerabilityNode = (VulnerabilityNode) node;
                String cveId = vulnerabilityNode.getCve().getCveId();
                if (vulnerabilityNode.getCve() == null || StringUtils.isBlank(cveId)) {
                    continue;
                }
                issues.putIfAbsent(cveId, new ArrayList<>());
                issues.get(cveId).add(vulnerabilityNode);
            }
        }
        return issues;
    }
}
