package com.jfrog.ide.idea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class ScanTimeLabelAction extends ToolbarLabelAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Long lastScanTime = LocalComponentsTree.getInstance(project).lastScanTime();
        boolean isScanInProgress = ScanManager.getInstance(project).isScanInProgress();
        if (!isScanInProgress && lastScanTime != null) {
            DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
            LocalDateTime lastScanString = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastScanTime), ZoneId.systemDefault());
            e.getPresentation().setText("Scanned: " + format.format(lastScanString));
        } else {
            e.getPresentation().setText("");
        }
    }
}
