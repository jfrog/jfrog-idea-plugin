package com.jfrog.ide.idea.ui.export;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.exporter.Exporter;
import com.jfrog.ide.idea.log.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;

/**
 * Represents export violated licenses to CSV menu button action.
 *
 * @author yahavi
 **/
class ExportCsvViolatedLicenses extends ExportCsv {
    private static final String DEFAULT_VIOLATED_LICENSES_FILE_NAME = "violated-licenses.csv";

    public ExportCsvViolatedLicenses(Project project) {
        super(project, "Violated Licenses");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Exporter csvExporter = createCsvExporter(project);
            if (csvExporter != null) {
                exportIfNeeded(project, csvExporter.generateViolatedLicensesReport(), DEFAULT_VIOLATED_LICENSES_FILE_NAME);
            }
        } catch (Exception exception) {
            Logger.getInstance().error(ExceptionUtils.getRootCauseMessage(exception), exception);
        }
    }
}
