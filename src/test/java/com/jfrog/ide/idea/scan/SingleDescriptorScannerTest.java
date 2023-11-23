package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.nodes.LicenseViolationNode;
import com.jfrog.ide.common.nodes.subentities.Severity;
import org.junit.Test;

import java.util.*;

public class SingleDescriptorScannerTest extends BasePlatformTestCase {
    public void testGroupDependenciesToDescriptorNodes() {
        final String ROOT_ID = "npm-example:0.0.3";
        final String ROOT_PATH = "/path/to/project";
        final String SEND_COMP_ID = "send:0.5.0";
        final String DEBUG_COMP_ID = "debug:1.0.2";
        final String MS_COMP_ID = "ms:0.6.2";

        DepTree depTree = new DepTree(ROOT_ID, new HashMap<>() {{
            put(ROOT_ID, new DepTreeNode().descriptorFilePath(ROOT_PATH + "/package.json").children(Set.of(SEND_COMP_ID, MS_COMP_ID)));
            put(SEND_COMP_ID, new DepTreeNode().children(Set.of(DEBUG_COMP_ID, MS_COMP_ID)));
            put(DEBUG_COMP_ID, new DepTreeNode().children(Set.of()));
            put(MS_COMP_ID, new DepTreeNode().children(Set.of()));
        }});
        Map<String, Set<String>> parents = ScannerBase.getParents(depTree);
        List<DependencyNode> vulnerableDeps = new ArrayList<>() {{
            add(new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("npm://" + SEND_COMP_ID));
            add(new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("npm://" + DEBUG_COMP_ID));
            add(new DependencyNode() {{
                addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));
            }}.componentId("npm://" + MS_COMP_ID));
        }};

        NpmScanner scanner = new NpmScanner(getProject(), ROOT_PATH, null, null);
        List<FileTreeNode> fileTreeNodes = scanner.groupDependenciesToDescriptorNodes(vulnerableDeps, depTree, parents);
        assertEquals(1, fileTreeNodes.size());

        FileTreeNode descriptorNode = fileTreeNodes.get(0);
        List<DependencyNode> depNodes = descriptorNode.getChildren().stream().map(treeNode -> (DependencyNode) treeNode).toList();
        assertEquals(3, descriptorNode.getChildren().size());

        DependencyNode sendDepNode = depNodes.stream().filter(depNode -> depNode.getComponentId().equals("npm://" + SEND_COMP_ID)).findFirst().get();
        assertFalse(sendDepNode.isIndirect());
        DependencyNode debugDepNode = depNodes.stream().filter(depNode -> depNode.getComponentId().equals("npm://" + DEBUG_COMP_ID)).findFirst().get();
        assertTrue(debugDepNode.isIndirect());
        DependencyNode msDepNode = depNodes.stream().filter(depNode -> depNode.getComponentId().equals("npm://" + MS_COMP_ID)).findFirst().get();
        assertFalse(msDepNode.isIndirect());
    }
}
