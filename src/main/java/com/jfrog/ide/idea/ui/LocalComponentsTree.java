package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBMenu;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.SortableChildrenTreeNode;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.navigation.NavigationTarget;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
public class LocalComponentsTree extends ComponentsTree {
    private static final String SHOW_IN_PROJECT_DESCRIPTOR = "Show direct dependency in project descriptor";

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

    public void addScanResults(List<FileTreeNode> fileTreeNodes) {
        for (FileTreeNode node : fileTreeNodes) {
            fileNodes.add(node);
            appendProjectWhenReady(node);
        }
    }

    private void appendProject(FileTreeNode filteredRoot) {
        SortableChildrenTreeNode root;
        if (getModel() == null) {
            root = new SortableChildrenTreeNode();
        } else {
            root = (SortableChildrenTreeNode) getModel().getRoot();
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

    public void addRightClickListener() {
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleContextMenu(LocalComponentsTree.this, e);
            }
        };
        addMouseListener(mouseListener);
    }

    private void handleContextMenu(ComponentsTree tree, MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        // Event is right-click.
        TreePath selectedPath = tree.getPathForRow(tree.getClosestRowForLocation(e.getX(), e.getY()));
        if (selectedPath == null) {
            return;
        }
        if (selectedPath.getLastPathComponent() instanceof DependencyNode) {
            createNodePopupMenu((DependencyNode) selectedPath.getLastPathComponent());
            popupMenu.show(tree, e.getX(), e.getY());
        }
    }


    private void createNodePopupMenu(DependencyNode selectedNode) {
        popupMenu.removeAll();
        NavigationService navigationService = NavigationService.getInstance(project);
        Set<NavigationTarget> navigationCandidates = navigationService.getNavigation(selectedNode);

        addNodeNavigation(navigationCandidates);
    }

    private void addNodeNavigation(Set<NavigationTarget> navigationCandidates) {
        if (navigationCandidates.size() > 1) {
            addMultiNavigation(navigationCandidates);
        } else {
            addSingleNavigation(navigationCandidates.iterator().next());
        }
    }

    private void addSingleNavigation(NavigationTarget navigationTarget) {
        popupMenu.add(createNavigationMenuItem(navigationTarget, SHOW_IN_PROJECT_DESCRIPTOR + " (" + navigationTarget.getComponentName() + ")"));
    }

    private void addMultiNavigation(Set<NavigationTarget> navigationCandidates) {
        JMenu multiMenu = new JBMenu();
        multiMenu.setText(SHOW_IN_PROJECT_DESCRIPTOR);
        for (NavigationTarget navigationTarget : navigationCandidates) {
            multiMenu.add(createNavigationMenuItem(navigationTarget, navigationTarget.getComponentName()));
        }
        popupMenu.add(multiMenu);
    }

    private JMenuItem createNavigationMenuItem(NavigationTarget navigationTarget, String headLine) {
        return new JBMenuItem(new AbstractAction(headLine) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!(navigationTarget.getElement() instanceof Navigatable)) {
                    return;
                }
                Navigatable navigatable = (Navigatable) navigationTarget.getElement();
                if (navigatable.canNavigate()) {
                    navigatable.navigate(true);
                }
            }
        });
    }
}
