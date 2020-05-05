package com.jfrog.ide.idea.inspections;

import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependenciesTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Bar Belity on 27/04/2020.
 * Manage navigation from node in Issues-tree to its corresponding item in the project descriptor.
 */
public class NavigationService {

    private Map<DependenciesTree, Set<PsiElement>> navigationMap = Maps.newHashMap();

    public static NavigationService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, NavigationService.class);
    }

    /**
     * Add a navigation element to the node in tree.
     * @param treeNode The tree-node to register the navigation from.
     * @param navigationTarget Target element in the project descriptor.
     */
    public void addNavigation(DependenciesTree treeNode, PsiElement navigationTarget) {
        Set<PsiElement> navigationTargets;
        navigationTargets = navigationMap.get(treeNode);
        if (navigationTargets == null) {
            navigationTargets = new HashSet<>(Collections.singletonList(navigationTarget));
            navigationMap.put(treeNode, navigationTargets);
            return;
        }
        if (navigationTargets.contains(navigationTarget)) {
            return;
        }
        navigationTargets.add(navigationTarget);
    }

    /**
     * Get navigation targets for a specific node in tree.
     * @param treeNode The tree-node to get its navigation.
     * @return Set of candidates for navigation.
     */
    public Set<PsiElement> getNavigation(DependenciesTree treeNode) {
        return navigationMap.get(treeNode);
    }

    /**
     * Get a navigable ancestor of a DependenciesTree node, in the issues tree.
     * @param node To find its navigable ancestor.
     * @return The first navigable ancestor of 'node', null of not found.
     */
    public DependenciesTree getNavigableParent(DependenciesTree node) {
        DependenciesTree parentCandidate = node;
        while (parentCandidate != null) {
            if (navigationMap.get(parentCandidate) != null) {
                return parentCandidate;
            }
            parentCandidate = (DependenciesTree) parentCandidate.getParent();
        }
        return null;
    }
}
