package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.filter.FilterManager;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.ui.filters.builds.BuildsMenu;
import com.jfrog.ide.idea.ui.filters.filtermanager.CiFilterManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

/**
 * @author yahavi
 */
public class CiComponentsTree extends ComponentsTree {
    private BuildsMenu buildsMenu;

    public CiComponentsTree(@NotNull Project project) {
        super(project);
    }

    public static CiComponentsTree getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CiComponentsTree.class);
    }

    public void setBuildsMenu(BuildsMenu buildsMenu) {
        this.buildsMenu = buildsMenu;
    }

    public void populateTree(DependencyTree root) {
        super.populateTree(root);
        buildsMenu.refresh();
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
        busConnection.subscribe(ProjectEvents.ON_SCAN_CI_CHANGE, this::applyFilters);
    }

    @Override
    protected void appendProjectWhenReady(DependencyTree filteredRoot) {
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
    @Override
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
        appendProjectWhenReady(filteredRoot);
    }
}
