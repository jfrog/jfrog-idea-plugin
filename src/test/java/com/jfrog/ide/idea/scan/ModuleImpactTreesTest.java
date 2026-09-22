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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleImpactTreesTest {
    private static final String PROJECT_ROOT_ID = "multi-project";
    private static final String MULTI1_COMP_ID = "org.jfrog.test:multi1:3.7.x-SNAPSHOT";
    private static final String MULTI3_COMP_ID = "org.jfrog.test:multi3:3.7.x-SNAPSHOT";
    private static final String SPRING_AOP_COMP_ID = "org.springframework:spring-aop:2.5.6";
    private static final String LOG4J_COMP_ID = "log4j:log4j:1.2.17";
    private static final String ABSENT_COMP_ID = "com.example:absent:1.0";

    @Test
    public void testPathIsBuiltOnlyFromTheModuleThatResolvesTheDependency() {
        DependencyNode springAop = vulnerableDependency(SPRING_AOP_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(SPRING_AOP_COMP_ID, springAop), multiModuleDepTree());

        Assert.assertEquals(List.of(PROJECT_ROOT_ID + " -> " + MULTI3_COMP_ID + " -> " + SPRING_AOP_COMP_ID),
                impactPaths(springAop));
    }

    @Test
    public void testEveryModuleThatResolvesTheDependencyContributesAPath() {
        DependencyNode log4j = vulnerableDependency(LOG4J_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(LOG4J_COMP_ID, log4j), multiModuleDepTree());

        List<String> paths = impactPaths(log4j);
        Assert.assertEquals("log4j is resolved by both modules: " + paths, 2, paths.size());
        Assert.assertTrue(paths.contains(PROJECT_ROOT_ID + " -> " + MULTI1_COMP_ID + " -> " + LOG4J_COMP_ID));
        Assert.assertTrue(paths.contains(PROJECT_ROOT_ID + " -> " + MULTI3_COMP_ID + " -> " + LOG4J_COMP_ID));
    }

    @Test
    public void testProjectRootIsNotPrependedWhenTheModuleIsTheProjectRoot() {
        DepTree singleModule = new DepTree(MULTI1_COMP_ID, multi1Nodes(),
                List.of(new DepTreeModule(MULTI1_COMP_ID, multi1Nodes())));
        DependencyNode log4j = vulnerableDependency(LOG4J_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(LOG4J_COMP_ID, log4j), singleModule);

        Assert.assertEquals(List.of(MULTI1_COMP_ID + " -> " + LOG4J_COMP_ID), impactPaths(log4j));
    }

    @Test
    public void testDependencyNoModuleResolvesStillGetsAnImpactTree() {
        DependencyNode absent = vulnerableDependency(ABSENT_COMP_ID);
        ScannerBase.populateImpactTrees(Map.of(ABSENT_COMP_ID, absent), multiModuleDepTree());

        Assert.assertNotNull("a dependency no module resolves must not be left without an impact tree", absent.getImpactTree());
        Assert.assertEquals(List.of(PROJECT_ROOT_ID + " -> " + ABSENT_COMP_ID), impactPaths(absent));
    }

    private DepTree multiModuleDepTree() {
        Map<String, DepTreeNode> merged = new HashMap<>();
        merged.putAll(multi1Nodes());
        merged.putAll(multi3Nodes());
        merged.put(PROJECT_ROOT_ID, new DepTreeNode().children(Set.of(MULTI1_COMP_ID, MULTI3_COMP_ID)));
        return new DepTree(PROJECT_ROOT_ID, merged,
                List.of(new DepTreeModule(MULTI1_COMP_ID, multi1Nodes()), new DepTreeModule(MULTI3_COMP_ID, multi3Nodes())));
    }

    private Map<String, DepTreeNode> multi1Nodes() {
        Map<String, DepTreeNode> nodes = new HashMap<>();
        nodes.put(MULTI1_COMP_ID, new DepTreeNode().children(Set.of(LOG4J_COMP_ID)));
        nodes.put(LOG4J_COMP_ID, new DepTreeNode());
        return nodes;
    }

    private Map<String, DepTreeNode> multi3Nodes() {
        Map<String, DepTreeNode> nodes = new HashMap<>();
        nodes.put(MULTI3_COMP_ID, new DepTreeNode().children(Set.of(LOG4J_COMP_ID, SPRING_AOP_COMP_ID)));
        nodes.put(LOG4J_COMP_ID, new DepTreeNode());
        nodes.put(SPRING_AOP_COMP_ID, new DepTreeNode());
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
