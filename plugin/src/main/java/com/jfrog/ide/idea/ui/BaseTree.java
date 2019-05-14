package com.jfrog.ide.idea.ui;

import com.intellij.ui.treeStructure.Tree;
import com.jfrog.ide.idea.utils.ProjectsMap;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.TreeModel;

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

    public void populateTree(TreeModel issuesTreeModel) {
        setModel(issuesTreeModel);
        validate();
        repaint();
    }

    public void reset() {
        projects = new ProjectsMap();
        removeAll();
    }

    public void addScanResults(String projectName, DependenciesTree dependenciesTree) {
        projects.put(projectName, dependenciesTree);
    }

    public abstract void applyFilters(ProjectsMap.ProjectKey projectName);
}
