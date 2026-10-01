package com.jfrog.ide.idea.scan.utils;

import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;

import java.util.*;

public class ImpactTreeBuilder {
    /**
     * Builds impact paths for {@link DependencyNode} objects.
     *
     * @param vulnerableDependencies a map of component IDs and the {@link DependencyNode} object matching each of them.
     *                               Impact paths will be built for these DependencyNodes
     * @param parents                a map of all dependencies and their parents
     * @param rootId                 the project's root component ID
     */
    public static void populateImpactTrees(Map<String, DependencyNode> vulnerableDependencies, Map<String, Set<String>> parents, String rootId) {
        populateImpactTrees(vulnerableDependencies, parents, rootId, null);
    }

    /**
     * Builds impact paths for {@link DependencyNode} objects, within a single module of the project.
     *
     * @param vulnerableDependencies a map of component IDs and the {@link DependencyNode} object matching each of them
     * @param parents                a map of the module's dependencies and their parents
     * @param rootId                 the module's root component ID, where every impact path ends
     * @param projectRootId          the project's root component ID to prepend to each path, or null when the
     *                               module root is already the project root
     */
    public static void populateImpactTrees(Map<String, DependencyNode> vulnerableDependencies, Map<String, Set<String>> parents, String rootId, String projectRootId) {
        for (DependencyNode vulnDep : vulnerableDependencies.values()) {
            String componentId = vulnDep.getComponentIdWithoutPrefix();
            if (!componentId.equals(rootId) && !parents.containsKey(componentId)) {
                continue;
            }
            walkParents(vulnDep, parents, rootId, Collections.singletonList(componentId), projectRootId);
        }
    }

    private static void walkParents(DependencyNode depNode, Map<String, Set<String>> parents, String rootId, List<String> path, String projectRootId) {
        String currParentId = path.get(0);
        if (depNode.getImpactTree() != null && depNode.getImpactTree().getImpactPathsCount() >= ImpactTree.IMPACT_PATHS_LIMIT) {
            return;
        }
        if (currParentId.equals(rootId)) {
            addImpactPathToDependencyNode(depNode, prependProjectRoot(path, projectRootId));
            return;
        }
        for (String grandparentId : parents.getOrDefault(currParentId, Collections.emptySet())) {
            if (path.contains(grandparentId)) {
                continue;
            }
            List<String> pathToGrandparent = new ArrayList<>(path);
            pathToGrandparent.add(0, grandparentId);
            walkParents(depNode, parents, rootId, pathToGrandparent, projectRootId);
        }
    }

    private static List<String> prependProjectRoot(List<String> path, String projectRootId) {
        if (projectRootId == null) {
            return path;
        }
        List<String> fullPath = new ArrayList<>(path);
        fullPath.add(0, projectRootId);
        return fullPath;
    }

    public static void addImpactPathToDependencyNode(DependencyNode dependencyNode, List<String> path) {
        if (dependencyNode.getImpactTree() == null) {
            dependencyNode.setImpactTree(new ImpactTree(new ImpactTreeNode(path.get(0))));
        }
        ImpactTree impactTree = dependencyNode.getImpactTree();
        if (impactTree.getImpactPathsCount() >= ImpactTree.IMPACT_PATHS_LIMIT) {
            return;
        }
        ImpactTreeNode parentImpactTreeNode = impactTree.getRoot();
        for (int pathNodeIndex = 1; pathNodeIndex < path.size(); pathNodeIndex++) {
            String currPathNode = path.get(pathNodeIndex);
            // Find a child of parentImpactTreeNode with a name equals to currPathNode
            ImpactTreeNode currImpactTreeNode = parentImpactTreeNode.getChildren().stream().filter(impactTreeNode -> impactTreeNode.getName().equals(currPathNode)).findFirst().orElse(null);
            if (currImpactTreeNode == null) {
                currImpactTreeNode = new ImpactTreeNode(currPathNode);
                parentImpactTreeNode.getChildren().add(currImpactTreeNode);
                if (pathNodeIndex == path.size() - 1) {
                    // If a new leaf was added, thus a new impact path was added (impact paths don't collide after they split)
                    impactTree.incImpactPathsCount();
                }
            }
            parentImpactTreeNode = currImpactTreeNode;
        }
    }
}
