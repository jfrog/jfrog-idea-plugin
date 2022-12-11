package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.tree.BaseTreeNode;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.*;

/**
 * @author yahavi
 */
public class LocalComponentsTree extends ComponentsTree {
    TreeMap<String, FileTreeNode> fileNodes = new TreeMap<>();

    public LocalComponentsTree(@NotNull Project project) {
        super(project);
        setCellRenderer(new LocalTreeCellRenderer());
    }

    public static LocalComponentsTree getInstance(@NotNull Project project) {
        return project.getService(LocalComponentsTree.class);
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        // TODO: consider removing the event logic
        busConnection.subscribe(ProjectEvents.ON_SCAN_PROJECT_CHANGE, this::applyFilters);
    }

    public void applyFilters(ProjectsMap.ProjectKey projectKey) {
        // TODO: temporary change! consider removing the event logic
        applyFilters(projectKey.getProjectName());
    }

    // TODO: change name
    public void applyFilters(String projectKey) {
        FileTreeNode project = fileNodes.get(projectKey);
        if (project == null) {
            return;
        }
        appendProjectWhenReady(project);

        // TODO: uncomment:
//        DumbService.getInstance(this.project).smartInvokeLater(() -> ScanManagersFactory.getInstance(this.project).runInspectionsForAllScanManagers());
    }

    private void appendProjectWhenReady(FileTreeNode filteredRoot) {
        ApplicationManager.getApplication().invokeLater(() -> appendProject(filteredRoot));
    }

    @Override
    public void reset() {
        fileNodes = new TreeMap<>();
        super.reset();
    }

    @Override
    public void applyFiltersForAllProjects() {
        setModel(null);
        // TODO: I added the reset here, but it might not be the right solution, and i'm not even sure if it's needed. If it's removed or moved from here, pay attention to the commented code in addOrReplace below.
        reset();
        for (Map.Entry<String, FileTreeNode> entry : fileNodes.entrySet()) {
            applyFilters(entry.getKey());
        }
    }

    public void addScanResults(String projectName, FileTreeNode fileTreeNode) {
        fileNodes.put(projectName, fileTreeNode);
    }

    private void appendProject(FileTreeNode filteredRoot) {
        // TODO: remove
//        // No projects in tree - Add filtered root as a single project and show only its children.
        BaseTreeNode root;
        if (getModel() == null) {
            root = new BaseTreeNode();
        } else {
            root = (BaseTreeNode) getModel().getRoot();
        }

        // Two or more projects in tree - Append filtered root to the empty parent node.
        root.add(filteredRoot);
        root.sortChildren();
        populateTree(root);
    }

    private void populateTree(DefaultMutableTreeNode root) {
        toolbarPopupMenus.forEach(ToolbarPopupMenu::refresh);
        setModel(new DefaultTreeModel(root));
        validate();
        repaint();
    }
}
