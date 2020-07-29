package com.jfrog.ide.idea.ui;

import junit.framework.TestCase;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author yahavi
 */
public class ComponentsTreeTest extends TestCase {

    private static final DependenciesTree[] EMPTY_TREE_NODE = new DependenciesTree[0];
    private ComponentsTree componentsTree;
    private DependenciesTree dependenciesTreeA;
    private DependenciesTree dependenciesTreeB;
    private DependenciesTree dependenciesTreeC;

    @Before
    public void setUp() {
        componentsTree = new BaseTreeImpl();
        dependenciesTreeA = createNode("a");
        dependenciesTreeB = createNode("b");
        dependenciesTreeC = createNode("c");
    }

    public void testOneProject() {
        // Append single project and check results
        componentsTree.appendProject(dependenciesTreeA);
        Assert.assertEquals(componentsTree.getModel().getRoot(), dependenciesTreeA);

        // Reset tree
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append single project again and check results
        componentsTree.appendProject(dependenciesTreeA);
        Assert.assertEquals(componentsTree.getModel().getRoot(), dependenciesTreeA);
    }

    public void testTwoProjects() {
        // Append 2 projects and check results
        componentsTree.appendProject(dependenciesTreeA);
        componentsTree.appendProject(dependenciesTreeB);
        DependenciesTree root = (DependenciesTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependenciesTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        DependenciesTree[] actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        DependenciesTree[] expected = {dependenciesTreeA, dependenciesTreeB};
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);

        // Reset tree and try again
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append 2 projects again and check results
        componentsTree.appendProject(dependenciesTreeA);
        componentsTree.appendProject(dependenciesTreeB);
        root = (DependenciesTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependenciesTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(new DependenciesTree[0]);
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);
    }

    public void testThreeProjects() {
        // Append 3 projects and check results
        componentsTree.appendProject(dependenciesTreeA);
        componentsTree.appendProject(dependenciesTreeB);
        componentsTree.appendProject(dependenciesTreeC);
        DependenciesTree root = (DependenciesTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependenciesTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        DependenciesTree[] actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        DependenciesTree[] expected = {dependenciesTreeA, dependenciesTreeB, dependenciesTreeC};
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);

        // Reset tree and try again
        componentsTree.reset();
        Assert.assertNull(componentsTree.getModel());

        // Append 3 projects again and check results
        componentsTree.appendProject(dependenciesTreeA);
        componentsTree.appendProject(dependenciesTreeB);
        componentsTree.appendProject(dependenciesTreeC);
        root = (DependenciesTree) componentsTree.getModel().getRoot();
        // If we have more the one project, the root node isn't a DependenciesTree node. It shouldn't contain a user object.
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(EMPTY_TREE_NODE);
        // Check root node's children
        Assert.assertArrayEquals(actual, expected);
    }

    private DependenciesTree createNode(String name) {
        GeneralInfo generalInfo = new GeneralInfo().name(name);
        DependenciesTree node = new DependenciesTree(name);
        node.setGeneralInfo(generalInfo);
        return node;
    }
}
