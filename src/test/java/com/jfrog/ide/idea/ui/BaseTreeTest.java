package com.jfrog.ide.idea.ui;

import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author yahavi
 */
public class BaseTreeTest {

    private BaseTree baseTree;
    private DependenciesTree dependenciesTreeA;
    private DependenciesTree dependenciesTreeB;
    private DependenciesTree dependenciesTreeC;

    @BeforeMethod
    public void setUp() {
        baseTree = new BaseTreeImpl();
        dependenciesTreeA = createNode("a");
        dependenciesTreeB = createNode("b");
        dependenciesTreeC = createNode("c");
    }

    @Test
    void oneProjectTest() {
        // Append single project and check results
        baseTree.appendProject(dependenciesTreeA);
        Assert.assertEquals(baseTree.getModel().getRoot(), dependenciesTreeA);

        // Reset tree
        baseTree.reset();
        Assert.assertNull(baseTree.getModel());

        // Append single project again and check results
        baseTree.appendProject(dependenciesTreeA);
        Assert.assertEquals(baseTree.getModel().getRoot(), dependenciesTreeA);
    }

    @Test
    void twoProjectsTest() {
        // Append 2 projects and check results
        baseTree.appendProject(dependenciesTreeA);
        baseTree.appendProject(dependenciesTreeB);
        DependenciesTree root = (DependenciesTree) baseTree.getModel().getRoot();
        Assert.assertNull(root.getUserObject());
        DependenciesTree[] actual = root.getChildren().toArray(new DependenciesTree[0]);
        DependenciesTree[] expected = {dependenciesTreeA, dependenciesTreeB};
        Assert.assertEqualsNoOrder(actual, expected);

        // Reset tree
        baseTree.reset();
        Assert.assertNull(baseTree.getModel());

        // Append 2 projects again and check results
        baseTree.appendProject(dependenciesTreeA);
        baseTree.appendProject(dependenciesTreeB);
        root = (DependenciesTree) baseTree.getModel().getRoot();
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(new DependenciesTree[0]);
        Assert.assertEqualsNoOrder(actual, expected);
    }

    @Test
    void threeProjectsTest() {
        // Append 3 projects and check results
        baseTree.appendProject(dependenciesTreeA);
        baseTree.appendProject(dependenciesTreeB);
        baseTree.appendProject(dependenciesTreeC);
        DependenciesTree root = (DependenciesTree) baseTree.getModel().getRoot();
        Assert.assertNull(root.getUserObject());
        DependenciesTree[] actual = root.getChildren().toArray(new DependenciesTree[0]);
        DependenciesTree[] expected = {dependenciesTreeA, dependenciesTreeB, dependenciesTreeC};
        Assert.assertEqualsNoOrder(actual, expected);

        // Reset tree
        baseTree.reset();
        Assert.assertNull(baseTree.getModel());

        // Append 3 projects again and check results
        baseTree.appendProject(dependenciesTreeA);
        baseTree.appendProject(dependenciesTreeB);
        baseTree.appendProject(dependenciesTreeC);
        root = (DependenciesTree) baseTree.getModel().getRoot();
        Assert.assertNull(root.getUserObject());
        actual = root.getChildren().toArray(new DependenciesTree[0]);
        Assert.assertEqualsNoOrder(actual, expected);
    }

    private DependenciesTree createNode(String name) {
        GeneralInfo generalInfo = new GeneralInfo().name(name);
        DependenciesTree node = new DependenciesTree(name);
        node.setGeneralInfo(generalInfo);
        return node;
    }
}
