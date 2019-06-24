package com.jfrog.ide.idea.ui.licenses;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.filters.LicenseFilterMenu;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.TreeModel;

/**
 * @author yahavi
 */
public class LicensesTree extends BaseTree {

    private LicenseFilterMenu licenseFilterMenu;

    public static LicensesTree getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, LicensesTree.class);
    }

    private LicensesTree(@NotNull Project mainProject) {
        super(mainProject);
        setCellRenderer(new LicensesTreeCellRenderer());
    }

    public void populateTree(TreeModel licensesTreeModel) {
        super.populateTree(licensesTreeModel);
        if (licenseFilterMenu != null) {
            licenseFilterMenu.setLicenses();
        }
    }

    void setLicenseFilterMenu(LicenseFilterMenu licenseFilterMenu) {
        this.licenseFilterMenu = licenseFilterMenu;
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        busConnection.subscribe(ProjectEvents.ON_SCAN_PROJECT_LICENSES_CHANGE, this::applyFilters);
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectName) {
        DependenciesTree dependenciesTree = projects.get(projectName);
        if (dependenciesTree == null) {
            return;
        }
        DependenciesTree filteredRoot = (DependenciesTree) dependenciesTree.clone();
        FilterManager filterManager = FilterManagerService.getInstance(mainProject);
        filterManager.applyFilters(dependenciesTree, new DependenciesTree(), filteredRoot);
        appendProjectToTree(filteredRoot);
    }

}
