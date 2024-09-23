package com.jfrog.ide.idea.navigation;

import com.google.common.collect.Maps;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jfrog.ide.common.nodes.DependencyNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 27/04/2020.
 * Manage navigation from node in Issues-tree to its corresponding item in the project descriptor.
 */
public class NavigationService {

    private final Map<DependencyNode, Set<NavigationTarget>> navigationMap = Maps.newHashMap();

    public static NavigationService getInstance(@NotNull Project project) {
        return project.getService(NavigationService.class);
    }

    /**
     * Clear existing navigation map.
     */
    public static void clearNavigationMap(@NotNull Project project) {
        NavigationService navigationService = NavigationService.getInstance(project);
        navigationService.navigationMap.clear();
    }

    /**
     * Add a navigation element to the node in tree.
     *
     * @param treeNode                The tree-node to register the navigation from.
     * @param navigationTargetElement The PsiElement we register the navigation to.
     */
    public void addNavigation(DependencyNode treeNode, PsiElement navigationTargetElement, String componentName) {
        PsiFile containingFile = navigationTargetElement.getContainingFile();
        FileViewProvider fileViewProvider = containingFile.getViewProvider();
        Document document = fileViewProvider.getDocument();
        if (document == null) {
            return;
        }
        NavigationTarget navigationTarget = new NavigationTarget(navigationTargetElement, document.getLineNumber(navigationTargetElement.getTextOffset()), componentName);
        Set<NavigationTarget> navigationTargets = navigationMap.get(treeNode);
        if (navigationTargets == null) {
            navigationTargets = new HashSet<>(Collections.singletonList(navigationTarget));

            navigationMap.put(treeNode, navigationTargets);
            return;
        }
        navigationTargets.add(navigationTarget);
    }

    /**
     * Get navigation targets for a specific node in tree.
     *
     * @param treeNode The tree-node to get its navigation.
     * @return Set of candidates for navigation.
     */
    public Set<NavigationTarget> getNavigation(DependencyNode treeNode) {
        return navigationMap.get(treeNode);
    }
}
