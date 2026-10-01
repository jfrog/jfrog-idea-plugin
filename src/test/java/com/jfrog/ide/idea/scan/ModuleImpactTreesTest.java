package com.jfrog.ide.idea.scan;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeModule;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Two modules resolving the same component differently: 'includes' keeps commons-lang3 under
 * commons-text, 'excludes' drops it. The merged tree holds the union of both, so only a walk that
 * respects module boundaries can tell that commons-lang3 is unreachable from 'excludes'.
 */
public class ModuleImpactTreesTest {
    private static final String PROJECT_ROOT_ID = "multi-project";
    private static final String INCLUDES_COMP_ID = "org.jfrog.test:includes:1.0";
    private static final String EXCLUDES_COMP_ID = "org.jfrog.test:excludes:1.0";
    private static final String COMMONS_TEXT_COMP_ID = "org.apache.commons:commons-text:1.9";
    private static final String COMMONS_LANG3_COMP_ID = "org.apache.commons:commons-lang3:3.11";
    private static final String UNREACHABLE_COMP_ID = "com.example:unreachable:1.0";

    @Test
    public void testExcludedTransitiveIsNotAttributedToTheModuleThatExcludesIt() {
        DependencyNode commonsLang3 = vulnerableDependency(COMMONS_LANG3_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(COMMONS_LANG3_COMP_ID, commonsLang3), multiModuleDepTree());

        Assert.assertEquals(
                List.of(PROJECT_ROOT_ID + " -> " + INCLUDES_COMP_ID + " -> " + COMMONS_TEXT_COMP_ID + " -> " + COMMONS_LANG3_COMP_ID),
                impactPaths(commonsLang3));
    }

    @Test
    public void testEveryModuleThatResolvesTheDependencyContributesAPath() {
        DependencyNode commonsText = vulnerableDependency(COMMONS_TEXT_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(COMMONS_TEXT_COMP_ID, commonsText), multiModuleDepTree());

        List<String> paths = impactPaths(commonsText);
        Assert.assertEquals("commons-text is resolved by both modules: " + paths, 2, paths.size());
        Assert.assertTrue(paths.contains(PROJECT_ROOT_ID + " -> " + INCLUDES_COMP_ID + " -> " + COMMONS_TEXT_COMP_ID));
        Assert.assertTrue(paths.contains(PROJECT_ROOT_ID + " -> " + EXCLUDES_COMP_ID + " -> " + COMMONS_TEXT_COMP_ID));
    }

    @Test
    public void testProjectRootIsNotPrependedWhenTheModuleIsTheProjectRoot() {
        DepTree singleModule = new DepTree(INCLUDES_COMP_ID, includesModuleNodes(),
                List.of(new DepTreeModule(INCLUDES_COMP_ID, includesModuleNodes())));
        DependencyNode commonsLang3 = vulnerableDependency(COMMONS_LANG3_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(COMMONS_LANG3_COMP_ID, commonsLang3), singleModule);

        Assert.assertEquals(List.of(INCLUDES_COMP_ID + " -> " + COMMONS_TEXT_COMP_ID + " -> " + COMMONS_LANG3_COMP_ID),
                impactPaths(commonsLang3));
    }

    @Test
    public void testDependencyNoModuleResolvesStillGetsAnImpactTree() {
        DependencyNode unreachable = vulnerableDependency(UNREACHABLE_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(UNREACHABLE_COMP_ID, unreachable), multiModuleDepTree());

        Assert.assertNotNull("a dependency no module resolves must not be left without an impact tree", unreachable.getImpactTree());
        Assert.assertEquals(List.of(PROJECT_ROOT_ID + " -> " + UNREACHABLE_COMP_ID), impactPaths(unreachable));
    }

    /**
     * Mirrors {@code GradleTreeBuilder.createDependencyTrees}: the merged map unions the children of a
     * component across modules, while each module keeps the children it resolved itself.
     */
    private DepTree multiModuleDepTree() {
        Map<String, DepTreeNode> merged = new HashMap<>();
        mergeInto(merged, includesModuleNodes());
        mergeInto(merged, excludesModuleNodes());
        merged.put(PROJECT_ROOT_ID, new DepTreeNode().children(new HashSet<>(Set.of(INCLUDES_COMP_ID, EXCLUDES_COMP_ID))));
        return new DepTree(PROJECT_ROOT_ID, merged,
                List.of(new DepTreeModule(INCLUDES_COMP_ID, includesModuleNodes()),
                        new DepTreeModule(EXCLUDES_COMP_ID, excludesModuleNodes())));
    }

    private void mergeInto(Map<String, DepTreeNode> merged, Map<String, DepTreeNode> moduleNodes) {
        moduleNodes.forEach((compId, node) ->
                merged.computeIfAbsent(compId, id -> new DepTreeNode()).getChildren().addAll(node.getChildren()));
    }

    private Map<String, DepTreeNode> includesModuleNodes() {
        Map<String, DepTreeNode> nodes = new HashMap<>();
        nodes.put(INCLUDES_COMP_ID, new DepTreeNode().children(new HashSet<>(Set.of(COMMONS_TEXT_COMP_ID))));
        nodes.put(COMMONS_TEXT_COMP_ID, new DepTreeNode().children(new HashSet<>(Set.of(COMMONS_LANG3_COMP_ID))));
        nodes.put(COMMONS_LANG3_COMP_ID, new DepTreeNode());
        return nodes;
    }

    private Map<String, DepTreeNode> excludesModuleNodes() {
        Map<String, DepTreeNode> nodes = new HashMap<>();
        nodes.put(EXCLUDES_COMP_ID, new DepTreeNode().children(new HashSet<>(Set.of(COMMONS_TEXT_COMP_ID))));
        nodes.put(COMMONS_TEXT_COMP_ID, new DepTreeNode());
        return nodes;
    }

    private DependencyNode vulnerableDependency(String componentId) {
        return new DependencyNode().componentId("gav://" + componentId);
    }

    private List<String> impactPaths(DependencyNode dependency) {
        List<String> paths = new ArrayList<>();
        collectPaths(dependency.getImpactTree().getRoot(), "", paths);
        return paths;
    }

    private void collectPaths(ImpactTreeNode node, String prefix, List<String> paths) {
        String path = prefix.isEmpty() ? node.getName() : prefix + " -> " + node.getName();
        if (node.getChildren().isEmpty()) {
            paths.add(path);
            return;
        }
        node.getChildren().forEach(child -> collectPaths(child, path, paths));
    }
}
