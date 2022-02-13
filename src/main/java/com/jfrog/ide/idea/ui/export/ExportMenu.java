package com.jfrog.ide.idea.ui.export;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Represents the export menu in the toolbar.
 *
 * @author yahavi
 **/
public class ExportMenu extends ToolbarPopupMenu {
    public ExportMenu(@NotNull Project project) {
        super(project, "Export", "Export", AllIcons.ToolbarDecorator.Export);
    }

    @Override
    public void refresh() {
        // Clean older menu items
        removeAll();

        // Get scan managers. If empty, the menu should be empty too.
        Set<ScanManager> scanManagers = ScanManagersFactory.getScanManagers(project);
        if (CollectionUtils.isEmpty(scanManagers)) {
            return;
        }

        // Add export vulnerabilities button only if there are issues in the tree
        if (scanManagers.stream().anyMatch(ScanManager::isContainIssues)) {
            add(new ExportCsvVulnerabilities(project));
        }

        // Add export violated licenses button only if there are violated licenses in the tree
        if (scanManagers.stream().anyMatch(ScanManager::isContainViolatedLicenses)) {
            add(new ExportCsvViolatedLicenses(project));
        }
    }
}
