package com.jfrog.ide.idea.scan.utils;

import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;

import java.util.*;

public class ImpactTreeBuilder {
    public static final int IMPACT_PATHS_LIMIT = 20;

    /**
     * Builds impact paths for {@link DependencyNode} objects.
     *
     * @param vulnerableDependencies a map of component IDs and the DependencyNode object matching each of them
     * @param parents                a map of all dependencies and their parents
     * @param rootId                 the project's root component ID
     * @return list of {@link FileTreeNode}s which are all {@link DescriptorFileTreeNode}s with the given
     * {@link DependencyNode}s inside them
     */
    public static void populateImpactTrees(Map<String, DependencyNode> vulnerableDependencies, Map<String, Set<String>> parents, String rootId) {
        for (DependencyNode vulnDep : vulnerableDependencies.values()) {
            walkParents(vulnDep, parents, rootId, Collections.singletonList(vulnDep.getComponentIdWithoutPrefix()));
        }
    }

    /**
     * Walks through a {@link DependencyNode}'s parents to build its impact paths.
     *
     * @param depNode         a vulnerable dependency
     * @param parents         a map of all dependencies and their parents
     * @param rootId          the project's root component ID
     * @param path            a path of nodes (represented by their component IDs) from the current parent to the current node
     */
    private static void walkParents(DependencyNode depNode, Map<String, Set<String>> parents, String rootId, List<String> path) {
        String currParentId = path.get(0);
        if (depNode.getImpactTree() != null && depNode.getImpactTree().getImpactPathsCount() >= IMPACT_PATHS_LIMIT) {
            return;
        }
        // If we arrived at the root, add the path to the impact tree
        if (currParentId.equals(rootId)) {
            addImpactPathToDependencyNode(depNode, path);
        } else {
            for (String grandparentId : parents.get(currParentId)) {
                if (path.contains(grandparentId)) {
                    continue;
                }
                List<String> pathToGrandparent = new ArrayList<>(path);
                pathToGrandparent.add(0, grandparentId);
                walkParents(depNode, parents, rootId, pathToGrandparent);
            }
        }
    }

    static void addImpactPathToDependencyNode(DependencyNode dependencyNode, List<String> path) {
        if (dependencyNode.getImpactTree() == null) {
            dependencyNode.setImpactTree(new ImpactTree(new ImpactTreeNode(path.get(0))));
        }
        ImpactTree impactTree = dependencyNode.getImpactTree();
        impactTree.incImpactPathsCount();
        if (impactTree.getImpactPathsCount() > IMPACT_PATHS_LIMIT) {
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
            }
            parentImpactTreeNode = currImpactTreeNode;
        }
    }
}
