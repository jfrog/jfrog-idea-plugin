package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.tree.BaseTreeNode;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yahavi
 */
public class LocalComponentsTree extends ComponentsTree {
    List<FileTreeNode> fileNodes = new ArrayList<>();

    public LocalComponentsTree(@NotNull Project project) {
        super(project);
        setCellRenderer(new ComponentsTreeCellRenderer());
    }

    public static LocalComponentsTree getInstance(@NotNull Project project) {
        return project.getService(LocalComponentsTree.class);
    }

    private void appendProjectWhenReady(FileTreeNode filteredRoot) {
        ApplicationManager.getApplication().invokeLater(() -> appendProject(filteredRoot));
    }

    @Override
    public void reset() {
        fileNodes = new ArrayList<>();
        super.reset();
    }

    @Override
    public void applyFiltersForAllProjects() {
        setModel(null);
        // TODO: I added the reset here, but it might not be the right solution, and i'm not even sure if it's needed. If it's removed or moved from here, pay attention to the commented code in addOrReplace below.
        reset();
        for (FileTreeNode node : fileNodes) {
            appendProjectWhenReady(node);
        }
    }

    public void addScanResults(List<FileTreeNode> fileTreeNodes) {
        for (FileTreeNode node : fileTreeNodes) {
            fileNodes.add(node);
            appendProjectWhenReady(node);
        }
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
