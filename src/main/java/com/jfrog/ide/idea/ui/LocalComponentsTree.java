package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBMenu;
import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.persistency.ScanCacheObject;
import com.jfrog.ide.idea.actions.CreateIgnoreRuleAction;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.navigation.NavigationTarget;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.ui.menus.ToolbarPopupMenu;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public class LocalComponentsTree extends ComponentsTree {
    public static final String IGNORE_RULE_TOOL_TIP = "Creating Ignore Rules is only available when a JFrog Project or Watch is defined.";
    private static final String SHOW_IN_PROJECT_DESCRIPTOR = "Show direct dependency in project descriptor";
    private static final String NO_ISSUES = "Your project was scanned and we didn't find any security issues.";
    private static final String ERROR_WHILE_SCANNING = "An error occurred while your project was scanned. Please see the Notifications tab for more details.";

    private static final String SCANNING = "Scanning...";
    private static final long EXPIRED_CACHE_TIME = TimeUnit.DAYS.toMillis(7); // week

    private final ScanCache cache;

    public LocalComponentsTree(@NotNull Project project) throws IOException {
        super(project);
        String projectId = project.getProjectFilePath();
        if (StringUtils.isBlank(projectId)) {
            projectId = project.getName();
        }
        cache = new ScanCache(projectId, Utils.HOME_PATH.resolve("cache"), Logger.getInstance());
        setNodesFromCache();
    }

    public static LocalComponentsTree getInstance(@NotNull Project project) {
        return project.getService(LocalComponentsTree.class);
    }

    public void addScanResults(List<FileTreeNode> fileTreeNodes) {
        setCellRenderer(new ComponentsTreeCellRenderer());
        ApplicationManager.getApplication().invokeLater(() -> doAddScanResults(fileTreeNodes));
    }

    /**
     * The primary logic of adding scan results to the components tree.
     * NOTE: This method must be run inside EDT. It's recommended to use {@link #addScanResults(List)} instead.
     *
     * @param fileTreeNodes File nodes to add to the components tree.
     */
    void doAddScanResults(List<FileTreeNode> fileTreeNodes) {
        SortableChildrenTreeNode root = getModel() != null ? (SortableChildrenTreeNode) getModel().getRoot() : new SortableChildrenTreeNode();
        for (FileTreeNode node : fileTreeNodes) {
            FileTreeNode existingNode =(FileTreeNode) Optional.ofNullable(root.getChildren())
                    .orElseGet(Vector::new).stream()
                    .filter(treeNode -> ((FileTreeNode) treeNode).getFilePath().equals(node.getFilePath()))
                    .findFirst().orElse(null);
            if (existingNode != null) {
                existingNode.mergeFileTreeNode(node);
                continue;
            }
            root.add(node);
        }
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
        Object selected = selectedPath.getLastPathComponent();

        // Create the popup menu if clicked on a package. if it's a vulnerability, create ignore rule option.
        if (selected instanceof DependencyNode) {
            DescriptorFileTreeNode descriptorFileTreeNode = (DescriptorFileTreeNode) selectedPath.getParentPath().getLastPathComponent();
            String descriptorPath =  descriptorFileTreeNode.getSubtitle();
            createNodePopupMenu((DependencyNode) selected, descriptorPath);
        } else if (selected instanceof VulnerabilityNode) {
            createIgnoreRuleOption((VulnerabilityNode) selected, e);
        } else if (selected instanceof ApplicableIssueNode) {
            createIgnoreRuleOption(((ApplicableIssueNode) selected).getIssue(), e);
        } else {
            return;
        }
        popupMenu.show(tree, e.getX(), e.getY());
    }


    private void createIgnoreRuleOption(VulnerabilityNode selectedIssue, MouseEvent mouseEvent) {
        popupMenu.removeAll();
        popupMenu.add(new CreateIgnoreRuleAction(selectedIssue.getIgnoreRuleUrl(), mouseEvent));
        JToolTip toolTip = popupMenu.createToolTip();
        toolTip.setToolTipText(IGNORE_RULE_TOOL_TIP);
        toolTip.setEnabled(true);
    }

    private void createNodePopupMenu(DependencyNode selectedNode, String descriptorPath) {
        popupMenu.removeAll();
        NavigationService navigationService = NavigationService.getInstance(project);
        Set<NavigationTarget> navigationCandidates = navigationService.getNavigation(selectedNode);
       //filtering candidates in case of multi module project
        Set<NavigationTarget> filteredCandidates = navigationCandidates.stream()
                .filter(navigationTarget ->
                        descriptorPath.equals(navigationTarget.getElement()
                                .getContainingFile()
                                .getVirtualFile()
                                .getPath()))
                .collect(Collectors.toSet());

        addNodeNavigation(filteredCandidates);
    }

    private void addNodeNavigation(Set<NavigationTarget> navigationCandidates) {
        if (navigationCandidates == null) {
            return;
        }
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
                if (!(navigationTarget.getElement() instanceof Navigatable navigatable)) {
                    return;
                }
                if (navigatable.canNavigate()) {
                    navigatable.navigate(true);
                }
            }
        });
    }

    public void cacheTree() throws IOException {
        if (getModel() == null) {
            cache.cacheNodes(new ArrayList<>());
            return;
        }
        SortableChildrenTreeNode root = (SortableChildrenTreeNode) getModel().getRoot();
        //noinspection unchecked
        cache.cacheNodes((List<FileTreeNode>) (List<?>) Collections.list(root.children()));
    }

    public void deleteCachedTree() throws IOException {
        cache.deleteScanCacheObject();
    }

    private void setNodesFromCache() {
        ScanCacheObject cacheObject = cache.getScanCacheObject();
        if (cacheObject == null) {
            return;
        }
        List<FileTreeNode> treeNodes = cacheObject.getFileTreeNodes();
        if (treeNodes == null) {
            setNoIssuesEmptyText();
            return;
        }
        SortableChildrenTreeNode root = new SortableChildrenTreeNode();
        for (FileTreeNode node : treeNodes) {
            root.add(node);
        }
        populateTree(root);

        if (isCacheExpired()) {
            // If cache is expired, display a gray tree and don't show inspections.
            // The reason for not showing inspections is to avoid displaying outdated results in the package descriptor.
            setCellRenderer(new ExpiredComponentsTreeCellRenderer());
            return;
        }
        setCellRenderer(new ComponentsTreeCellRenderer());

        // Run inspections after loaded cache
        ScanManager.getInstance(project).runInspections(project);
    }

    public boolean isCacheEmpty() {
        return cache.getScanCacheObject() == null;
    }

    public boolean isCacheExpired() {
        return System.currentTimeMillis() - cache.getScanCacheObject().getScanTimestamp() >= EXPIRED_CACHE_TIME;
    }

    public Long lastScanTime() {
        if (isCacheEmpty()) {
            return null;
        }
        return cache.getScanCacheObject().getScanTimestamp();
    }

    /**
     * Sets the empty text to "Scanning...".
     * It means that this text will be shown only if the tree is empty.
     */
    public void setScanningEmptyText() {
        SwingUtilities.invokeLater(() -> getEmptyText().setText(SCANNING));
    }

    /**
     * Sets the empty text to indicate that the project was scanned and no issues were found.
     * It means that this indication will be shown only if the tree is empty.
     */
    public void setNoIssuesEmptyText() {
        SwingUtilities.invokeLater(() -> getEmptyText().setText(NO_ISSUES));
    }

    /**
     * Sets the empty text to indicate that during the project scan an error occurred.
     */
    public void setScanErrorEmptyText() {
        SwingUtilities.invokeLater(() -> getEmptyText().setText(ERROR_WHILE_SCANNING));
    }
}