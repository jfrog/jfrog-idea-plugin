package com.jfrog.ide.idea.scan.utils;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeModule;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;

import java.util.*;

public class ImpactTreeBuilder {
    /**
     * Builds impact paths for {@link DependencyNode} objects, walking each module's own tree when the project has modules.
     *
     * @param vulnerableDependencies a map of component IDs and the {@link DependencyNode} object matching each of them
     * @param depTree                the project's dependency tree
     */
    public static void populateImpactTrees(Map<String, DependencyNode> vulnerableDependencies, DepTree depTree) {
        if (depTree.modules().isEmpty()) {
            populateImpactTrees(vulnerableDependencies, getParents(depTree.nodes()), depTree.rootId());
        } else {
            for (DepTreeModule module : depTree.modules()) {
                String projectRootId = module.rootId().equals(depTree.rootId()) ? null : depTree.rootId();
                populateImpactTrees(vulnerableDependencies, getParents(module.nodes()), module.rootId(), projectRootId);
            }
        }
        addMissingImpactTrees(vulnerableDependencies, depTree.rootId());
    }

    /**
     * Find the parents of each node. Nodes without parents (the root) don't appear in the returned map.
     *
     * @param nodes a map of component IDs and their {@link DepTreeNode}
     * @return a map of the nodes and each one's parents
     */
    public static Map<String, Set<String>> getParents(Map<String, DepTreeNode> nodes) {
        Map<String, Set<String>> parents = new HashMap<>();
        for (Map.Entry<String, DepTreeNode> node : nodes.entrySet()) {
            String parentId = node.getKey();
            for (String childId : node.getValue().getChildren()) {
                parents.putIfAbsent(childId, new HashSet<>());
                parents.get(childId).add(parentId);
            }
        }
        return parents;
    }

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
     * Builds impact paths for {@link DependencyNode} objects within a single module of the project.
     *
     * @param vulnerableDependencies a map of component IDs and the {@link DependencyNode} object matching each of them
     * @param parents                a map of the module's dependencies and their parents
     * @param rootId                 the module's root component ID, where every impact path ends
     * @param projectRootId          the project's root component ID to prepend to each path, or null
     */
    private static void populateImpactTrees(Map<String, DependencyNode> vulnerableDependencies, Map<String, Set<String>> parents, String rootId, String projectRootId) {
        for (DependencyNode vulnDep : vulnerableDependencies.values()) {
            String componentId = vulnDep.getComponentIdWithoutPrefix();
            if (!componentId.equals(rootId) && !parents.containsKey(componentId)) {
                continue;
            }
            walkParents(vulnDep, parents, rootId, Collections.singletonList(componentId), projectRootId);
        }
    }

    /**
     * Walks through a {@link DependencyNode}'s parents to build its impact paths.
     *
     * @param depNode         a vulnerable dependency
     * @param parents         a map of all dependencies and their parents
     * @param rootId          the module's root component ID
     * @param path            a path of nodes (represented by their component IDs) from the current parent to the current node
     * @param projectRootId   the project's root component ID to prepend to each path, or null
     */
    private static void walkParents(DependencyNode depNode, Map<String, Set<String>> parents, String rootId, List<String> path, String projectRootId) {
        String currParentId = path.get(0);
        if (depNode.getImpactTree() != null && depNode.getImpactTree().getImpactPathsCount() >= ImpactTree.IMPACT_PATHS_LIMIT) {
            return;
        }
        // If we arrived at the root, add the path to the impact tree
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

    private static void addMissingImpactTrees(Map<String, DependencyNode> vulnerableDependencies, String rootId) {
        for (DependencyNode dependency : vulnerableDependencies.values()) {
            if (dependency.getImpactTree() == null) {
                addImpactPathToDependencyNode(dependency, List.of(rootId, dependency.getComponentIdWithoutPrefix()));
            }
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
