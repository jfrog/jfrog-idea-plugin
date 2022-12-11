package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import com.jfrog.ide.idea.ui.menus.builds.BuildsMenu;
import com.jfrog.ide.idea.ui.menus.filtermanager.CiFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.tree.DefaultTreeModel;
import java.util.Map;
import java.util.Vector;

/**
 * @author yahavi
 */
public class CiComponentsTree extends ComponentsTree {
    ProjectsMap projects = new ProjectsMap();
    private BuildsMenu buildsMenu;

    public CiComponentsTree(@NotNull Project project) {
        super(project);
    }

    public static CiComponentsTree getInstance(@NotNull Project project) {
        return project.getService(CiComponentsTree.class);
    }

    public void setBuildsMenu(BuildsMenu buildsMenu) {
        this.buildsMenu = buildsMenu;
    }

    private void populateTree(DependencyTree root) {
        toolbarPopupMenus.forEach(ToolbarPopupMenu::refresh);
        setModel(new DefaultTreeModel(root));
        validate();
        repaint();
        setCellRenderer(new ComponentsTreeCellRenderer());
        buildsMenu.refresh();
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        busConnection.subscribe(ProjectEvents.ON_SCAN_CI_CHANGE, this::applyFilters);
    }

    private void appendProjectWhenReady(DependencyTree filteredRoot) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (CollectionUtils.size(filteredRoot.getChildren()) == 1) {
                appendProject(filteredRoot.getChildren().get(0));
            } else {
                appendProject(filteredRoot);
            }
        });
    }

    /**
     * Apply filters for the given project. If projectKey is null, clean the tree and the builds menu.
     *
     * @param projectKey - The project to apply the filters.
     */
// TODO: remove?
    //    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectKey) {
        if (projectKey == null) {
            reset();
            buildsMenu.refresh();
            return;
        }
        DependencyTree project = projects.get(projectKey);
        if (project == null) {
            return;
        }
        FilterManager filterManager = CiFilterManager.getInstance(this.project);
        DependencyTree filteredRoot = filterManager.applyFilters(project);
        filteredRoot.setIssues(filteredRoot.processTreeIssues());
        filteredRoot.setViolatedLicenses(filteredRoot.processTreeViolatedLicenses());
        appendProjectWhenReady(filteredRoot);
    }

    @Override
    public void reset() {
        projects = new ProjectsMap();
        super.reset();
    }

    @Override
    public void applyFiltersForAllProjects() {
        setModel(null);
        for (Map.Entry<ProjectsMap.ProjectKey, DependencyTree> entry : projects.entrySet()) {
            applyFilters(entry.getKey());
        }
    }

    public void addScanResults(String projectName, DependencyTree dependencyTree) {
        projects.put(projectName, dependencyTree);
    }

    private void appendProject(DependencyTree filteredRoot) {
        // No projects in tree - Add filtered root as a single project and show only its children.
        if (getModel() == null) {
            populateTree(filteredRoot);
            return;
        }

        DependencyTree root = (DependencyTree) getModel().getRoot();
        // One project in tree - Append filtered root and the old root the a new empty parent node.
        if (root.getUserObject() != null) {
            DependencyTree newRoot = filteredRoot;
            if (!Utils.areRootNodesEqual(root, filteredRoot)) {
                newRoot = new DependencyTree();
                newRoot.setMetadata(true);
                newRoot.add(root);
                newRoot.add(filteredRoot);
            }
            populateTree(newRoot);
            return;
        }

        // Two or more projects in tree - Append filtered root to the empty parent node.
        addOrReplace(root, filteredRoot);
        populateTree(root);
    }

    private void addOrReplace(DependencyTree root, DependencyTree filteredRoot) {
        int childIndex = searchNode(root, filteredRoot);
        if (childIndex >= 0) {
            root.remove(childIndex);
        }
        root.add(filteredRoot);
    }

    private int searchNode(DependencyTree root, DependencyTree filteredRoot) {
        Vector<DependencyTree> children = root.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (Utils.areRootNodesEqual(children.get(i), filteredRoot)) {
                return i;
            }
        }
        return -1;
    }
}
