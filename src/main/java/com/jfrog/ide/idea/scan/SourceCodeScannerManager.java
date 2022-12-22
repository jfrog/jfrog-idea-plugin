package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.common.tree.DescriptorFileTreeNode;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.common.tree.IssueTreeNode;
import com.jfrog.ide.idea.inspections.JfrogSecurityWarning;
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

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    private Eos eos = new Eos();
    private ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor();

    private List<JfrogSecurityWarning> scanResults;

    protected Project project;
    protected String codeBaseLanguage;

    public SourceCodeScannerManager(Project project, String codeBaseLanguage) {
        this.project = project;
        this.codeBaseLanguage = codeBaseLanguage.toString().toLowerCase();
    }

//    public void asyncScanAndUpdateResults() {
//        if (DumbService.isDumb(project)) { // If intellij is still indexing the project
//            return;
//        }
//        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, "JFrog source code scanning:") {
//            @Override
//            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
//                if (project.isDisposed()) {
//                    return;
//                }
//                // Prevent multiple simultaneous scans
//                if (!scanInProgress.compareAndSet(false, true)) {
//                    return;
//                }
//                try {
//                    scanAndUpdate(new ProgressIndicatorImpl(indicator), issuesMap.keySet());
//                } catch (IOException | InterruptedException | NullPointerException e) {
//                    logError(Logger.getInstance(), "Failed to scan source code", e, true);
//                } finally {
//                    scanInProgress.set(false);
//                    indicator.setFraction(1);
//                }
//            }
//
//        };
//        // The progress manager is only good for foreground threads.
//        if (SwingUtilities.isEventDispatchThread()) {
//            ProgressManager.getInstance().run(scanAndUpdateTask);
//        } else {
//            // Run the scan task when the thread is in the foreground.
//            ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(scanAndUpdateTask));
//        }
//    }

    /**
     * Source code scan and update components.
     *
     * @param indicator - The progress indicator
     * @param cves      - white list of cves to scan
     */
    public void scanAndUpdate(ProgressIndicator indicator, List<String> cves) throws IOException, InterruptedException {
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
                var applicabilityResults = applicability.execute(new ScanConfig.Builder().roots(List.of(project.getBasePath())).cves(cves));
                scanResults.addAll(applicabilityResults);
            }
            if (eos.getSupportedLanguages().contains(codeBaseLanguage)) {
                indicator.setText("Eos Scan");
                indicator.setFraction(0.5);
                var eosResults = eos.execute(new ScanConfig.Builder().language(codeBaseLanguage).roots(List.of(project.getBasePath())));
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
        var results = new HashMap<String, DescriptorFileTreeNode>();
        for (JfrogSecurityWarning warning : scanResults) {
            var filePath = StringUtils.removeStart(warning.getFilePath(), "file://");
            var fileNode = results.get(filePath);
            if (fileNode == null) {
                fileNode = new DescriptorFileTreeNode(filePath);
                results.put(filePath, fileNode);
            }
            var cve = StringUtils.removeStart(warning.getName(), "applic_");
            var issues = issuesMap.get(cve);
            if (issues != null) {
                fileNode.addDependency(new IssueTreeNode(cve, warning.getLineStart() + 1, warning.getColStart(), issues.get(0)));
                for (var issue : issues) {
                    // TODO: Add applicable scan info to the Issue object.
                }
            }
        }
        return new ArrayList<>(results.values());
    }
}
