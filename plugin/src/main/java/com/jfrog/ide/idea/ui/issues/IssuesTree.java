package com.jfrog.ide.idea.ui.issues;

import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
import com.jfrog.ide.idea.ui.renderers.IssuesTreeCellRenderer;
import com.jfrog.ide.idea.utils.ProjectsMap;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Map;

/**
 * @author yahavi
 */
public class IssuesTree extends BaseTree {

    private static IssuesTree instance;

    private IssuesTreeExpansionListener issuesTreeExpansionListener;

    public static IssuesTree getInstance() {
        if (instance == null) {
            instance = new IssuesTree();
        }
        return instance;
    }

    private IssuesTree() {
        super();
        setCellRenderer(new IssuesTreeCellRenderer());
    }

    void createExpansionListener(JPanel issuesCountPanel, Map<TreePath, JPanel> issuesCountPanels) {
        issuesTreeExpansionListener = new IssuesTreeExpansionListener(this, issuesCountPanel, issuesCountPanels);
    }

    void addTreeExpansionListener() {
        addTreeExpansionListener(issuesTreeExpansionListener);
    }

    public void populateTree(TreeModel issuesTreeModel) {
        super.populateTree(issuesTreeModel);
        issuesTreeExpansionListener.setIssuesCountPanel();
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectKey) {
        DependenciesTree project = projects.get(projectKey);
        if (project != null) {
            DependenciesTree filteredRoot = (DependenciesTree) project.clone();
            filteredRoot.getIssues().clear();
            FilterManager filterManager = FilterManager.getInstance();
            filterManager.applyFilters(project, filteredRoot, new DependenciesTree());
            filteredRoot.setIssues(filteredRoot.processTreeIssues());
            DependenciesTree root = ((DependenciesTree) getModel().getRoot());
            root.add(filteredRoot);
            if (root.getChildCount() == 1) {
                // If there is only one project - Show only its dependencies in the tree viewer.
                setModel(new DefaultTreeModel(filteredRoot));
            } else {
                setModel(new DefaultTreeModel(root));
            }
//            long totalIssues = root.getChildren().stream().mapToInt(DependenciesTree::getIssueCount).sum();
        }
    }

}
