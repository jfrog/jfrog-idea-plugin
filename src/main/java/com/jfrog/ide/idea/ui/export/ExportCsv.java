package com.jfrog.ide.idea.ui.export;

import com.intellij.ide.ExporterToTextFile;
import com.intellij.ide.util.ExportToFileUtil;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.exporter.Exporter;
import com.jfrog.ide.common.exporter.csv.CsvExporter;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    /**
     * Export CSV to file if the user clicked on the "save" button.
     *
     * @param project  - The project
     * @param content  - The CSV content to export
     * @param fileName - The full file name
     * @throws IOException in case of any unexpected I/O error during writing to the file or creating the directory.
     */
    void exportIfNeeded(Project project, String content, String fileName) throws IOException {
        ExporterToTextFile exportToFileOptions = new ExportToFileOptions(project, content, fileName);
        ExportToFileUtil.ExportDialogBase dialogBase = new ExportToFileUtil.ExportDialogBase(project, exportToFileOptions);
        boolean shouldExport = dialogBase.showAndGet();

        if (shouldExport) {
            File file = new File(dialogBase.getFileName());
            FileUtils.forceMkdirParent(file);
            FileUtils.write(file, content, StandardCharsets.UTF_8);
        }
    }
}
