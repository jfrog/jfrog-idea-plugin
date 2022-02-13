package com.jfrog.ide.idea.ui.export;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.exporter.Exporter;
import com.jfrog.ide.idea.log.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.awt.event.ActionEvent;

/**
 * Represents export vulnerabilities to CSV menu button action.
 *
 * @author yahavi
 **/
class ExportCsvVulnerabilities extends ExportCsv {
    private static final String DEFAULT_VULNERABILITIES_FILE_NAME = "vulnerabilities.csv";

    public ExportCsvVulnerabilities(Project project) {
        super(project, "Vulnerabilities");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Exporter csvExporter = createCsvExporter(project);
            if (csvExporter != null) {
                exportIfNeeded(project, csvExporter.generateVulnerabilitiesReport(), DEFAULT_VULNERABILITIES_FILE_NAME);
            }
        } catch (Exception exception) {
            Logger.getInstance().error(ExceptionUtils.getRootCauseMessage(exception), exception);
        }
    }
}
