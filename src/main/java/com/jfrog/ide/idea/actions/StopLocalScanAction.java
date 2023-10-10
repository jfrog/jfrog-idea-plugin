package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.scan.ScanManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by romang on 3/6/17.
 */
public class StopLocalScanAction extends AnAction {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        ScanManager.getInstance(project).stopScan();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        boolean isScanInProgress = ScanManager.getInstance(project).isScanInProgress();
        e.getPresentation().setVisible(isScanInProgress);
    }
}
