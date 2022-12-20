package com.jfrog.ide.idea.scan;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.log.ProgressIndicator;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.scan.data.ScanConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.common.log.Utils.logError;

public class SourceCodeScannerManager {
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    private Eos eos = new Eos();
    private ApplicabilityScannerExecutor applicability = new ApplicabilityScannerExecutor();

    protected Project project;
    protected String codeBaseLanguage;

    public SourceCodeScannerManager(Project project, String codeBaseLanguage) {
        this.project = project;
        this.codeBaseLanguage = codeBaseLanguage.toString().toLowerCase();
    }

    public void asyncScanAndUpdateResults(boolean shouldToast) {
        if (DumbService.isDumb(project)) { // If intellij is still indexing the project
            return;
        }
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, "JFrog source code scanning:") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                if (project.isDisposed()) {
                    return;
                }
                // Prevent multiple simultaneous scans
                if (!scanInProgress.compareAndSet(false, true)) {
                    return;
                }
                try {
                    scanAndUpdate(new ProgressIndicatorImpl(indicator));
                } catch (IOException | InterruptedException | NullPointerException e) {
                    logError(Logger.getInstance(), "Failed to scan source code", e, shouldToast);
                } finally {
                    scanInProgress.set(false);
                    indicator.setFraction(1);
                }
            }

        };
        // The progress manager is only good for foreground threads.
        if (SwingUtilities.isEventDispatchThread()) {
            ProgressManager.getInstance().run(scanAndUpdateTask);
        } else {
            // Run the scan task when the thread is in the foreground.
            ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(scanAndUpdateTask));
        }
    }

    /**
     * Source code scan and update components.
     *
     * @param indicator - The progress indicator
     */
    private void scanAndUpdate(ProgressIndicator indicator) throws IOException, InterruptedException {
        indicator.setText("1/2: Applicability Scan");
        var applicabilityResults = applicability.execute(new ScanConfig.Builder().root(project.getBasePath()));
        indicator.setText("2/2: Eos Scan");
        var eosResults = eos.execute(new ScanConfig.Builder().language(codeBaseLanguage).roots(List.of(project.getBasePath())));
    }
}
