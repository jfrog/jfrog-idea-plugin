package com.jfrog.ide.idea.ui.licenses;

import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.filters.LicenseFilterMenu;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.TreeModel;

/**
 * @author yahavi
 */
public class LicensesTree extends BaseTree {

    private LicenseFilterMenu licenseFilterMenu;

    private static LicensesTree instance;

    public static LicensesTree getInstance() {
        if (instance == null) {
            instance = new LicensesTree();
        }
        return instance;
    }

    private LicensesTree() {
        super();
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
    public void applyFilters(ProjectsMap.ProjectKey projectName) {
        DependenciesTree project = projects.get(projectName);
        if (project == null) {
            return;
        }
        DependenciesTree filteredRoot = (DependenciesTree) project.clone();
        FilterManager filterManager = FilterManager.getInstance();
        filterManager.applyFilters(project, new DependenciesTree(), filteredRoot);
        appendProjectToTree(filteredRoot);
    }

}
