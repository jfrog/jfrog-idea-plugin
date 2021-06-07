package com.jfrog.ide.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.junit.Assert;

import static org.gradle.internal.impldep.org.testng.Assert.assertNotNull;

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
        assertNotNull(childNode, "Couldn't find node '" + childName + "' between " + node + ".");
        return childNode;
    }
}
