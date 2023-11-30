package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.LicenseViolationNode;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.idea.scan.utils.ImpactTreeBuilder;

import java.util.*;

public class MavenScannerTest extends BasePlatformTestCase {
    public void testGroupDependenciesToDescriptorNodes() {
        final String MULTI_DESC_PATH = "/path/to/project/maven-example/pom.xml";
        final String MULTI1_DESC_PATH = "/path/to/project/maven-example/multi1/pom.xml";
        final String MULTI2_DESC_PATH = "/path/to/project/maven-example/multi2/pom.xml";
        final String MULTI3_DESC_PATH = "/path/to/project/maven-example/multi3/pom.xml";

        final String MULTI_COMP_ID = "org.jfrog.test:multi:3.7.x-SNAPSHOT";
        final String MULTI1_COMP_ID = "org.jfrog.test:multi1:3.7.x-SNAPSHOT";
        final String MULTI2_COMP_ID = "org.jfrog.test:multi2:3.7.x-SNAPSHOT";
        final String MULTI3_COMP_ID = "org.jfrog.test:multi3:3.7.x-SNAPSHOT";
        final String SPRING_AOP_COMP_ID = "org.springframework:spring-aop:2.5.6";
        final String SPRING_CORE_COMP_ID = "org.springframework:spring-core:2.5.6";
        final String LOG4J_COMP_ID = "log4j:log4j:1.2.17";

        DepTree depTree = new DepTree(MULTI_COMP_ID, new HashMap<>() {{
            put(MULTI_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI_DESC_PATH).children(Set.of(MULTI1_COMP_ID, MULTI3_COMP_ID, LOG4J_COMP_ID)));
            put(MULTI1_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI1_DESC_PATH).children(Set.of(LOG4J_COMP_ID)));
            put(MULTI2_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI2_DESC_PATH).children(Set.of(SPRING_CORE_COMP_ID)));
            put(MULTI3_COMP_ID, new DepTreeNode().descriptorFilePath(MULTI3_DESC_PATH).children(Set.of(MULTI1_COMP_ID, LOG4J_COMP_ID, SPRING_AOP_COMP_ID)));
            put(SPRING_AOP_COMP_ID, new DepTreeNode().children(Set.of(SPRING_CORE_COMP_ID)));
            put(SPRING_CORE_COMP_ID, new DepTreeNode().children(Set.of()));
            put(LOG4J_COMP_ID, new DepTreeNode().children(Set.of()));
        }});
        Map<String, Set<String>> parents = ScannerBase.getParents(depTree);

        List<DependencyNode> vulnerableDeps = new ArrayList<>() {{
            add(new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("gav://" + LOG4J_COMP_ID));
            add(new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("gav://" + SPRING_AOP_COMP_ID));
        }};

        MavenScanner scanner = new MavenScanner(getProject(), null, null);
        List<FileTreeNode> fileTreeNodes = scanner.groupDependenciesToDescriptorNodes(vulnerableDeps, depTree, parents);
        assertEquals(3, fileTreeNodes.size());

        FileTreeNode multiDescFile = fileTreeNodes.stream().filter(fileTreeNode -> fileTreeNode.getFilePath().equals(MULTI_DESC_PATH)).findFirst().get();
        FileTreeNode multi1DescFile = fileTreeNodes.stream().filter(fileTreeNode -> fileTreeNode.getFilePath().equals(MULTI1_DESC_PATH)).findFirst().get();
        FileTreeNode multi3DescFile = fileTreeNodes.stream().filter(fileTreeNode -> fileTreeNode.getFilePath().equals(MULTI3_DESC_PATH)).findFirst().get();

        assertEquals(2, multiDescFile.getChildren().size());
        DependencyNode multiLog4jDepNode = multiDescFile.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).filter(depNode -> depNode.getComponentId().equals("gav://" + LOG4J_COMP_ID)).findFirst().get();
        assertFalse(multiLog4jDepNode.isIndirect());
        DependencyNode multiSpringAopDepNode = multiDescFile.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).filter(depNode -> depNode.getComponentId().equals("gav://" + SPRING_AOP_COMP_ID)).findFirst().get();
        assertTrue(multiSpringAopDepNode.isIndirect());

        assertEquals(1, multi1DescFile.getChildren().size());
        DependencyNode multi1Log4jDepNode = multi1DescFile.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).filter(depNode -> depNode.getComponentId().equals("gav://" + LOG4J_COMP_ID)).findFirst().get();
        assertFalse(multi1Log4jDepNode.isIndirect());

        assertEquals(2, multi3DescFile.getChildren().size());
        DependencyNode multi3Log4jDepNode = multi3DescFile.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).filter(depNode -> depNode.getComponentId().equals("gav://" + LOG4J_COMP_ID)).findFirst().get();
        assertFalse(multi3Log4jDepNode.isIndirect());
        DependencyNode multi3SpringAopDepNode = multi3DescFile.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).filter(depNode -> depNode.getComponentId().equals("gav://" + SPRING_AOP_COMP_ID)).findFirst().get();
        assertFalse(multi3SpringAopDepNode.isIndirect());
    }
}
