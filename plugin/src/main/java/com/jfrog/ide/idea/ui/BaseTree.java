package com.jfrog.ide.idea.ui;

import com.intellij.ui.treeStructure.Tree;
import com.jfrog.ide.idea.utils.ProjectsMap;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Map;

/**
 * @author yahavi
 */
public abstract class BaseTree extends Tree {

    protected ProjectsMap projects = new ProjectsMap();

    public BaseTree() {
        super(new DependenciesTree(null));
        expandRow(0);
        setRootVisible(false);
    }

    public abstract void applyFilters(ProjectsMap.ProjectKey projectName);

    public void populateTree(TreeModel issuesTreeModel) {
        setModel(issuesTreeModel);
        validate();
        repaint();
    }

    public void reset() {
        projects = new ProjectsMap();
        setModel(null);
    }

    public void addScanResults(String projectName, DependenciesTree dependenciesTree) {
        projects.put(projectName, dependenciesTree);
    }

    public void applyFiltersForAllProjects() {
        setModel(null);
        for (Map.Entry<ProjectsMap.ProjectKey, DependenciesTree> entry : projects.entrySet()) {
            applyFilters(entry.getKey());
        }
    }

    protected void appendProjectToTree(DependenciesTree filteredRoot) {
        SwingUtilities.invokeLater(() -> {
            // No projects in tree - Add filtered root as a single project and show only its children.
            if (getModel() == null) {
                populateTree(new DefaultTreeModel(filteredRoot));
                return;
            }

            DependenciesTree root = (DependenciesTree) getModel().getRoot();
            // One project in tree - Append filtered root and the old root the a new empty parent node.
            if (root.getUserObject() != null) {
                DependenciesTree newRoot = new DependenciesTree();
                newRoot.add(root);
                newRoot.add(filteredRoot);
                populateTree(new DefaultTreeModel(newRoot));
                return;
            }

            // Two or more projects in tree - Append filtered root to the empty parent node.
            root.add(filteredRoot);
            populateTree(new DefaultTreeModel(root));
        });
    }
}
