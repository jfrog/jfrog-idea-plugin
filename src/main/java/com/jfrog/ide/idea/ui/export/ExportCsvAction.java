package com.jfrog.ide.idea.ui.export;

import com.intellij.ide.ExporterToTextFile;
import com.intellij.ide.actions.ExportToTextFileAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Represents export to CSV dialog.
 *
 * @author yahavi
 **/
class ExportCsvAction extends ExportToTextFileAction implements ExporterToTextFile {
    private final Project project;
    private final String fileName;
    private final String content;

    ExportCsvAction(Project project, String fileName, String content) {
        this.project = project;
        this.fileName = fileName;
        this.content = content;
    }

    @Override
    protected ExporterToTextFile getExporter(DataContext dataContext) {
        return this;
    }

    @Override
    public @NotNull String getReportText() {
        try {
            return content;
        } catch (Exception e) {
            Logger.getInstance().error(ExceptionUtils.getRootCauseMessage(e), e);
        }
        return "";
    }

    @Override
    public @NotNull String getDefaultFilePath() {
        return Utils.getProjectBasePath(project).resolve(fileName).toString();
    }

    @Override
    public boolean canExport() {
        return true;
    }

    @Override
    public void exportedTo(@NotNull String filePath) {
        File file = new File(filePath);
        try {
            FileUtils.forceMkdirParent(file);
            FileUtils.write(file, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Logger.getInstance().error(ExceptionUtils.getRootCauseMessage(exception), exception);
        }
    }

    /**
     * Open the export CSV dialog if the user clicked on the "Vulnerabilities" or the "Violated Licenses" button.
     */
    void openDialog() {
        super.actionPerformed(new ExportCsvActionEvent());
    }

    private class ExportCsvActionEvent extends AnActionEvent {
        ExportCsvActionEvent() {
            super(null, dataId -> project, "", new Presentation(), ActionManager.getInstance(), 0);
        }
    }
}
