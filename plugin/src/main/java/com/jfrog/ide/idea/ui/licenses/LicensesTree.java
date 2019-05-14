package com.jfrog.ide.idea.ui.licenses;

import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.renderers.LicensesTreeCellRenderer;
import com.jfrog.ide.idea.utils.ProjectsMap;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

/**
 * @author yahavi
 */
public class LicensesTree extends BaseTree {

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
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectName) {
        DependenciesTree project = projects.get(projectName);
        if (project != null) {
            DependenciesTree filteredRoot = (DependenciesTree) project.clone();
            FilterManager filterManager = FilterManager.getInstance();
            filterManager.applyFilters(project, new DependenciesTree(), filteredRoot);
            DependenciesTree root = ((DependenciesTree) getModel().getRoot());
            root.add(filteredRoot);
            if (root.getChildCount() == 1) {
                // If there is only one project - Show only its dependencies in the tree viewer.
                setModel(new DefaultTreeModel(filteredRoot));
            } else {
                setModel(new DefaultTreeModel(root));
            }
        }
    }

}
