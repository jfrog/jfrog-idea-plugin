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
import javax.swing.tree.MutableTreeNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yahavi
 */
public class LocalComponentsTree extends ComponentsTree {
    Map<String, FileTreeNode> fileNodes = new HashMap<>();

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
        fileNodes = new HashMap<>();
        super.reset();
    }

    @Override
    public void applyFiltersForAllProjects() {
        setModel(null);
        // TODO: I added the reset here, but it might not be the right solution, and i'm not even sure if it's needed. If it's removed or moved from here, pay attention to the commented code in addOrReplace below.
        reset();
        for (FileTreeNode node : fileNodes.values()) {
            appendProjectWhenReady(node);
        }
    }

    public void addScanResults(List<FileTreeNode> fileTreeNodes) {
        for (FileTreeNode node : fileTreeNodes) {
            var fileNode = fileNodes.get(node.getPath().toString());
            if (fileNode != null) {
                List newChildren = Collections.list(fileNode.children());
                for (var child : newChildren) {
                    fileNode.add((MutableTreeNode) child);
                }
            } else {
                fileNodes.put(node.getPath().toString(), node);
                appendProjectWhenReady(node);
            }
        }
    }

    private void appendProject(FileTreeNode filteredRoot) {
        BaseTreeNode root;
        if (getModel() == null) {
            root = new BaseTreeNode();
        } else {
            root = (BaseTreeNode) getModel().getRoot();
        }

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
