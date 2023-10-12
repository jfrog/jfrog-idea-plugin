package com.jfrog.ide.idea.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.common.nodes.subentities.Severity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class LocalComponentsTreeTest extends HeavyPlatformTestCase {
    public void testAddScanResults() {
        LocalComponentsTree tree = LocalComponentsTree.getInstance(getProject());
        List<FileTreeNode> nodes1 = List.of(
                createFileTreeNodeWithIssue("path/to/file1.txt", "issue1"),
                createFileTreeNodeWithIssue("path/to/file2.txt", "issue2")
        );

        ApplicationManager.getApplication().invokeAndWait(() -> tree.doAddScanResults(nodes1));
        assertEquals(2, getTreeFileNodes(tree).size());
        List<FileTreeNode> nodes2 = List.of(
                createFileTreeNodeWithIssue("path/to/file1.txt", "issue3"),
                createFileTreeNodeWithIssue("path/to/file3.txt", "issue4")
        );
        ApplicationManager.getApplication().invokeAndWait(() -> tree.doAddScanResults(nodes2));
        List<FileTreeNode> actualNodes = getTreeFileNodes(tree);
        assertEquals(3, actualNodes.size());
        FileTreeNode file1Node = actualNodes.stream().filter(fileTreeNode -> fileTreeNode.getFilePath().equals("path/to/file1.txt")).findFirst().get();
        assertEquals(2, file1Node.getChildren().size());
    }

    private FileTreeNode createFileTreeNodeWithIssue(String filePath, String... issueNames) {
        FileTreeNode fileNode = new FileTreeNode(filePath);
        for (String issueName : issueNames) {
            SastIssueNode issueNode = new SastIssueNode(issueName, "filePath", 1, 1, 1, 1, "reason", "lineSnippet", null, Severity.High, "ruleID");
            fileNode.addIssue(issueNode);
        }
        return fileNode;
    }

    private List<FileTreeNode> getTreeFileNodes(LocalComponentsTree tree) {
        SortableChildrenTreeNode root = (SortableChildrenTreeNode) tree.getModel().getRoot();
        return (List<FileTreeNode>) (List<?>) Collections.list(root.children());
    }
}
