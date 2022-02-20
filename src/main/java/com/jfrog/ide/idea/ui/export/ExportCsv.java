package com.jfrog.ide.idea.ui.export;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.exporter.Exporter;
import com.jfrog.ide.common.exporter.csv.CsvExporter;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;

/**
 * Represents export to CSV menu button action. Base class for ExportCsvVulnerabilities and ExportCsvViolatedLicenses.
 *
 * @author yahavi
 **/
abstract class ExportCsv extends AbstractAction {
    final Project project;

    public ExportCsv(Project project, String name) {
        super(name, IconUtils.load("csv"));
        this.project = project;
    }

    /**
     * Create the CSV exporter, that generated CSV report of vulnerabilities and violated licenses.
     *
     * @param project - The project
     * @return the CSV exporter or null if a scan refresh is in progress.
     */
    Exporter createCsvExporter(Project project) {
        LocalComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        if (componentsTree.getModel() == null) {
            return null;
        }
        DependencyTree root = (DependencyTree) componentsTree.getModel().getRoot();
        return new CsvExporter(root);
    }
}
