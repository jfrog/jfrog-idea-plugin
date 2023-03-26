package com.jfrog.ide.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.junit.Assert;

import java.util.Enumeration;

import static com.intellij.testFramework.UsefulTestCase.assertNotEmpty;

/**
 * Created by Bar Belity on 11/06/2020.
 */
public class TestUtils {

    public static PsiElement getNonLeafElement(PsiFile fileDescriptor, Class<? extends PsiElement> psiClass, int position) {
        PsiElement element = fileDescriptor.findElementAt(position);
        Assert.assertNotNull(element);
        while (!(psiClass.isAssignableFrom(element.getClass()))) {
            element = element.getParent();
            Assert.assertNotNull(element);
        }
        return element;
    }

    /**
     * Get the dependency tree child. Fail the test if it doesn't exist.
     *
     * @param node      - The dependency tree
     * @param childName - The child name to search
     * @return the dependency tree child.
     */
    public static DependencyTree getAndAssertChild(DependencyTree node, String childName) {
        DependencyTree childNode = node.getChildren().stream()
                .filter(child -> childName.equals(child.getUserObject()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull("Couldn't find node '" + childName + "' as one of " + node + "'s children.", childNode);
        return childNode;
    }

    /**
     * Assert all nodes in the tree, except the root, contain scopes.
     *
     * @param root - The root dependency tree node
     */
    public static void assertScopes(DependencyTree root) {
        for (Enumeration<?> enumeration = root.depthFirstEnumeration(); enumeration.hasMoreElements(); ) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            if (child.getParent() == null) {
                // Skip the root node
                continue;
            }
            assertNotEmpty(child.getScopes());
        }
    }
}
