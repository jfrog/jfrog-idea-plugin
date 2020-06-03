package com.jfrog.ide.idea.ui.issues;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.ui.components.JBMenu;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.exclusion.Excludable;
import com.jfrog.ide.idea.exclusion.ExclusionUtils;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.navigation.NavigationTarget;
import com.jfrog.ide.idea.ui.BaseTree;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author yahavi
 */
public class IssuesTree extends BaseTree {

    private static final String SHOW_IN_PROJECT_DESCRIPTOR = "Show in project descriptor";
    private static final String EXCLUDE_DEPENDENCY = "Exclude dependency";
    private IssuesTreeExpansionListener issuesTreeExpansionListener;
    private JPanel issuesCountPanel;
    private JLabel issuesCount;
    private JBPopupMenu popupMenu = new JBPopupMenu();

    private IssuesTree(@NotNull Project mainProject) {
        super(mainProject);
        setCellRenderer(new IssuesTreeCellRenderer());
    }

    public static IssuesTree getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, IssuesTree.class);
    }

    void setIssuesCountLabel(JLabel issuesCount) {
        this.issuesCount = issuesCount;
    }

    void createExpansionListener(JPanel issuesCountPanel, Map<TreePath, JPanel> issuesCountPanels) {
        this.issuesCountPanel = issuesCountPanel;
        this.issuesTreeExpansionListener = new IssuesTreeExpansionListener(this, issuesCountPanel, issuesCountPanels);
    }

    void addTreeExpansionListener() {
        addTreeExpansionListener(issuesTreeExpansionListener);
    }

    public void populateTree(DependenciesTree root) {
        super.populateTree(root);
        issuesTreeExpansionListener.setIssuesCountPanel();
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        busConnection.subscribe(ProjectEvents.ON_SCAN_PROJECT_ISSUES_CHANGE, this::applyFilters);
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectKey) {
        DependenciesTree project = projects.get(projectKey);
        if (project == null) {
            return;
        }
        DependenciesTree filteredRoot = (DependenciesTree) project.clone();
        filteredRoot.getIssues().clear();
        FilterManager filterManager = FilterManagerService.getInstance(mainProject);
        filterManager.applyFilters(project, filteredRoot, new DependenciesTree());
        filteredRoot.setIssues(filteredRoot.processTreeIssues());
        appendProjectWhenReady(filteredRoot);
        calculateIssuesCount();
    }

    @Override
    public void applyFiltersForAllProjects() {
        resetIssuesCountPanels();
        super.applyFiltersForAllProjects();
    }

    @Override
    public void reset() {
        super.reset();
        resetIssuesCountPanels();
    }

    private void resetIssuesCountPanels() {
        if (issuesCount != null && issuesCountPanel != null) {
            issuesCount.setText("Issues (0) ");
            issuesCountPanel.removeAll();
        }
    }

    private void calculateIssuesCount() {
        ApplicationManager.getApplication().invokeLater(() -> {
            DependenciesTree root = (DependenciesTree) getModel().getRoot();
            int sum = root.getChildren().stream()
                    .map(DependenciesTree::getIssues)
                    .distinct()
                    .flatMapToInt(issues -> IntStream.of(issues.size()))
                    .sum();
            issuesCount.setText("Issues (" + sum + ") ");
        });
    }

    public void addRightClickListener() {
        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleContextMenu(IssuesTree.this, e);
            }
        };
        addMouseListener(mouseListener);
    }

    private void handleContextMenu(IssuesTree tree, MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        // Event is right-click.
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) {
            return;
        }
        createNodePopupMenu((DependenciesTree) selPath.getLastPathComponent());
        popupMenu.show(tree, e.getX(), e.getY());
    }

    private void createNodePopupMenu(DependenciesTree selectedNode) {
        popupMenu.removeAll();
        NavigationService navigationService = NavigationService.getInstance(mainProject);
        Set<NavigationTarget> navigationCandidates = navigationService.getNavigation(selectedNode);
        DependenciesTree affectedNode = selectedNode;
        if (navigationCandidates == null) {
            // Find the direct dependency containing the selected dependency.
            affectedNode = navigationService.getNavigableParent(selectedNode);
            if (affectedNode == null) {
                return;
            }
            navigationCandidates = navigationService.getNavigation(affectedNode);
            if (navigationCandidates == null) {
                return;
            }
        }

        addNodeNavigation(navigationCandidates);
        addNodeExclusion(selectedNode, navigationCandidates, affectedNode);
    }

    private void addNodeNavigation(Set<NavigationTarget> navigationCandidates) {
        if (navigationCandidates.size() > 1) {
            addMultiNavigation(navigationCandidates);
        } else {
            addSingleNavigation(navigationCandidates.iterator().next());
        }
    }

    private void addSingleNavigation(NavigationTarget navigationTarget) {
        popupMenu.add(createNavigationMenuItem(navigationTarget, SHOW_IN_PROJECT_DESCRIPTOR));
    }

    private void addMultiNavigation(Set<NavigationTarget> navigationCandidates) {
        JMenu multiMenu = new JBMenu();
        multiMenu.setText(SHOW_IN_PROJECT_DESCRIPTOR);
        for (NavigationTarget navigationTarget : navigationCandidates) {
            String descriptorPath = getRelativizedDescriptorPath(navigationTarget);
            multiMenu.add(createNavigationMenuItem(navigationTarget, descriptorPath + " " + (navigationTarget.getLineNumber() + 1)));
        }
        popupMenu.add(multiMenu);
    }

    private String getRelativizedDescriptorPath(NavigationTarget navigationTarget) {
        String pathResult = "";
        try {
            VirtualFile descriptorVirtualFile = navigationTarget.getElement().getContainingFile().getVirtualFile();
            pathResult = descriptorVirtualFile.getName();
            String projBasePath = mainProject.getBasePath();
            if (projBasePath == null) {
                return pathResult;
            }
            Path basePath = Paths.get(mainProject.getBasePath());
            Path descriptorPath = Paths.get(descriptorVirtualFile.getPath());
            pathResult = basePath.relativize(descriptorPath).toString();
        } catch (InvalidPathException | PsiInvalidElementAccessException ex) {
            Logger log = Logger.getInstance(mainProject);
            log.error("Failed getting project-descriptor's path.", ex);
        }
        return pathResult;
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

    private void addNodeExclusion(DependenciesTree nodeToExclude, Set<NavigationTarget> parentCandidates, DependenciesTree affectedNode) {
        if (!ExclusionUtils.isExcludable(nodeToExclude, affectedNode)) {
            return;
        }
        if (parentCandidates.size() > 1) {
            addMultiExclusion(nodeToExclude, parentCandidates);
        } else {
            addSingleExclusion(nodeToExclude, parentCandidates.iterator().next());
        }
    }

    private void addMultiExclusion(DependenciesTree nodeToExclude, Set<NavigationTarget> parentCandidates) {
        JMenu multiMenu = new JBMenu();
        multiMenu.setText(EXCLUDE_DEPENDENCY);
        for (NavigationTarget parentCandidate : parentCandidates) {
            Excludable excludable = ExclusionUtils.getExcludable(nodeToExclude, parentCandidate);
            String descriptorPath = getRelativizedDescriptorPath(parentCandidate);
            multiMenu.add(createExcludeMenuItem(excludable, descriptorPath + " " + (parentCandidate.getLineNumber() + 1)));
        }
        popupMenu.add(multiMenu);
    }

    private void addSingleExclusion(DependenciesTree nodeToExclude, NavigationTarget parentCandidate) {
        Excludable excludable = ExclusionUtils.getExcludable(nodeToExclude, parentCandidate);
        popupMenu.add(createExcludeMenuItem(excludable, EXCLUDE_DEPENDENCY));
    }

    private JBMenuItem createExcludeMenuItem(Excludable excludable, String headLine) {
        return new JBMenuItem(new AbstractAction(headLine) {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludable.exclude(mainProject);
            }
        });
    }
}
