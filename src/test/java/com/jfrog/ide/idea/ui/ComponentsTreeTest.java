package com.jfrog.ide.idea.ui;

import junit.framework.TestCase;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.junit.Assert;
import org.junit.Before;

import java.nio.file.Paths;

/**
 * @author yahavi
 */
public class ComponentsTreeTest extends TestCase {

    private static final DependencyTree[] EMPTY_TREE_NODE = new DependencyTree[0];
    private ComponentsTree componentsTree;
    private DependencyTree dependencyTreeA;
    private DependencyTree dependencyTreeB;
    private DependencyTree DependencyTreeC;

    @Before
    public void setUp() {
        componentsTree = new BaseTreeImpl();
        dependencyTreeA = createNode("a");
        dependencyTreeB = createNode("b");
        DependencyTreeC = createNode("c");
    }

    public void testOneProject() {
        // Append single project and check results
        componentsTree.appendProject(dependencyTreeA);
        Assert.assertEquals(componentsTree.getModel().getRoot(), dependencyTreeA);

        // Reset tree
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append single project again and check results
        componentsTree.appendProject(dependencyTreeA);
        Assert.assertEquals(componentsTree.getModel().getRoot(), dependencyTreeA);
    }

    public void testTwoProjects() {
        // Append 2 projects and check results
        componentsTree.appendProject(dependencyTreeA);
        componentsTree.appendProject(dependencyTreeB);
        DependencyTree root = (DependencyTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependencyTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        DependencyTree[] actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        DependencyTree[] expected = {dependencyTreeA, dependencyTreeB};
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);

        // Reset tree and try again
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append 2 projects again and check results
        componentsTree.appendProject(dependencyTreeA);
        componentsTree.appendProject(dependencyTreeB);
        root = (DependencyTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependencyTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(new DependencyTree[0]);
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);
    }

    public void testThreeProjects() {
        // Append 3 projects and check results
        componentsTree.appendProject(dependencyTreeA);
        componentsTree.appendProject(dependencyTreeB);
        componentsTree.appendProject(DependencyTreeC);
        DependencyTree root = (DependencyTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependencyTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        DependencyTree[] actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        DependencyTree[] expected = {dependencyTreeA, dependencyTreeB, DependencyTreeC};
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);

        // Reset tree and try again
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append 3 projects again and check results
        componentsTree.appendProject(dependencyTreeA);
        componentsTree.appendProject(dependencyTreeB);
        componentsTree.appendProject(DependencyTreeC);
        root = (DependencyTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependencyTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);
    }

    private DependencyTree createNode(String componentId) {
        GeneralInfo generalInfo = new GeneralInfo().componentId(componentId)
                .path(Paths.get(".").resolve(componentId).toAbsolutePath().toString());
        DependencyTree node = new DependencyTree(componentId);
        node.setGeneralInfo(generalInfo);
        return node;
    }
}
