package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Map;
import java.util.Vector;

/**
 * @author yahavi
 */
public abstract class BaseTree extends Tree {

    protected ProjectsMap projects = new ProjectsMap();
    protected Project mainProject;

    public BaseTree(@NotNull Project mainProject) {
        super(new DependenciesTree(null));
        this.mainProject = mainProject;
        expandRow(0);
        setRootVisible(false);
    }

    protected abstract void addOnProjectChangeListener(MessageBusConnection busConnection);

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
        ApplicationManager.getApplication().invokeLater(() -> {
            // No projects in tree - Add filtered root as a single project and show only its children.
            if (getModel() == null) {
                populateTree(new DefaultTreeModel(filteredRoot));
                return;
            }

            DependenciesTree root = (DependenciesTree) getModel().getRoot();
            // One project in tree - Append filtered root and the old root the a new empty parent node.
            if (root.getUserObject() != null) {
                DependenciesTree newRoot = filteredRoot;
                if (!Utils.areRootNodesEqual(root, filteredRoot)) {
                    newRoot = new DependenciesTree();
                    newRoot.add(root);
                    newRoot.add(filteredRoot);
                }
                populateTree(new DefaultTreeModel(newRoot));
                return;
            }

            // Two or more projects in tree - Append filtered root to the empty parent node.
            addOrReplace(root, filteredRoot);
            populateTree(new DefaultTreeModel(root));
        });
    }

    private int searchNode(DependenciesTree root, DependenciesTree filteredRoot) {
        Vector<DependenciesTree> children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (Utils.areRootNodesEqual(children.get(i), filteredRoot)) {
                return i;
            }
        }
        return -1;
    }

    private void addOrReplace(DependenciesTree root, DependenciesTree filteredRoot) {
        int childIndex = searchNode(root, filteredRoot);
        if (childIndex < 0) {
            root.add(filteredRoot);
        } else {
            root.getChildren().set(childIndex, filteredRoot);
        }
    }
}
