package com.jfrog.ide.idea.ui.export;

import com.intellij.ide.ExporterToTextFile;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the export dialog options.
 *
 * @author yahavi
 **/
class ExportToFileOptions implements ExporterToTextFile {
    private final Project project;
    private final String fileName;
    private final String report;

    public ExportToFileOptions(Project project, String report, String fileName) {
        this.fileName = fileName;
        this.project = project;
        this.report = report;
    }

    @Override
    public @NotNull String getReportText() {
        try {
            return report;
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
}
