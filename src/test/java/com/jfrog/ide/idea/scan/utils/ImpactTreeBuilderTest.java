package com.jfrog.ide.idea.scan.utils;

import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.LicenseViolationNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.common.nodes.subentities.ImpactTreeNode;
import com.jfrog.ide.common.nodes.subentities.Severity;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class ImpactTreeBuilderTest {
    private final String MULTI_DESC_PATH = "/path/to/project/maven-example/pom.xml";
    private final String MULTI1_DESC_PATH = "/path/to/project/maven-example/multi1/pom.xml";
    private final String MULTI3_DESC_PATH = "/path/to/project/maven-example/multi3/pom.xml";

    private final String MULTI_COMP_ID = "org.jfrog.test:multi:3.7.x-SNAPSHOT";
    private final String MULTI1_COMP_ID = "org.jfrog.test:multi1:3.7.x-SNAPSHOT";
    private final String MULTI3_COMP_ID = "org.jfrog.test:multi3:3.7.x-SNAPSHOT";
    private final String SPRING_AOP_COMP_ID = "org.springframework:spring-aop:2.5.6";
    private final String SPRING_CORE_COMP_ID = "org.springframework:spring-core:2.5.6";
    private final String LOG4J_COMP_ID = "log4j:log4j:1.2.17";

    private Map<String, Set<String>> parents = new HashMap<>() {{
        put(MULTI1_COMP_ID, new HashSet<>() {{
            add(MULTI_COMP_ID);
            add(MULTI3_COMP_ID);
        }});
        put(MULTI3_COMP_ID, new HashSet<>() {{
            add(MULTI_COMP_ID);
        }});
        put(SPRING_AOP_COMP_ID, new HashSet<>() {{
            add(MULTI3_COMP_ID);
        }});
        put(SPRING_CORE_COMP_ID, new HashSet<>() {{
            add(SPRING_AOP_COMP_ID);
        }});
        put(LOG4J_COMP_ID, new HashSet<>() {{
            add(MULTI_COMP_ID);
            add(MULTI1_COMP_ID);
            add(MULTI3_COMP_ID);
        }});
    }};
    private ImpactTree log4jImpactTree = new ImpactTree(new ImpactTreeNode(MULTI_COMP_ID) {{
        getChildren().add(new ImpactTreeNode(LOG4J_COMP_ID));
        getChildren().add(new ImpactTreeNode(MULTI1_COMP_ID) {{
            getChildren().add(new ImpactTreeNode(LOG4J_COMP_ID));
        }});
        getChildren().add(new ImpactTreeNode(MULTI3_COMP_ID) {{
            getChildren().add(new ImpactTreeNode(LOG4J_COMP_ID));
            getChildren().add(new ImpactTreeNode(MULTI1_COMP_ID) {{
                getChildren().add(new ImpactTreeNode(LOG4J_COMP_ID));
            }});
        }});
    }});
    private ImpactTree springAopImpactTree = new ImpactTree(new ImpactTreeNode(MULTI_COMP_ID) {{
        getChildren().add(new ImpactTreeNode(MULTI3_COMP_ID) {{
            getChildren().add(new ImpactTreeNode(SPRING_AOP_COMP_ID));
        }});
    }});

    @Test
    public void testBuildImpactTrees() {
        Map<String, DependencyNode> vulnerableDeps = new HashMap<>() {{
            put(LOG4J_COMP_ID, new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("gav://" + LOG4J_COMP_ID));
            put(SPRING_AOP_COMP_ID, new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("gav://" + SPRING_AOP_COMP_ID));
        }};
        ImpactTreeBuilder.populateImpactTrees(vulnerableDeps, parents, MULTI_COMP_ID);

        assertImpactTree(log4jImpactTree, vulnerableDeps.get(LOG4J_COMP_ID).getImpactTree());
        assertImpactTree(springAopImpactTree, vulnerableDeps.get(SPRING_AOP_COMP_ID).getImpactTree());
    }

    @Test
    public void testAddImpactPathToDependencyNode() {
        DependencyNode depNode = new DependencyNode();
        ImpactTreeBuilder.addImpactPathToDependencyNode(depNode, List.of(MULTI_COMP_ID, LOG4J_COMP_ID));
        ImpactTreeBuilder.addImpactPathToDependencyNode(depNode, List.of(MULTI_COMP_ID, MULTI1_COMP_ID, LOG4J_COMP_ID));
        ImpactTreeBuilder.addImpactPathToDependencyNode(depNode, List.of(MULTI_COMP_ID, MULTI3_COMP_ID, LOG4J_COMP_ID));
        ImpactTreeBuilder.addImpactPathToDependencyNode(depNode, List.of(MULTI_COMP_ID, MULTI3_COMP_ID, MULTI1_COMP_ID, LOG4J_COMP_ID));
        assertImpactTree(log4jImpactTree, depNode.getImpactTree());
    }

    private static void assertImpactTree(ImpactTree expected, ImpactTree actual) {
        assertImpactTreeNode(expected.getRoot(), actual.getRoot());
    }

    private static void assertImpactTreeNode(ImpactTreeNode expected, ImpactTreeNode actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getChildren().size(), actual.getChildren().size());
        expected.getChildren().forEach(expectedNode -> {
            ImpactTreeNode actualNode = actual.getChildren().stream().filter(actualImpactTreeNode -> actualImpactTreeNode.getName().equals(expectedNode.getName())).findFirst().get();
            assertImpactTreeNode(actualNode, expectedNode);
        });
    }
}
