package com.jfrog.ide.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

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

    public static <T extends PsiElement> List<T> findElementsOfType(PsiFile file, Class<T> type) {
        return new ArrayList<>(PsiTreeUtil.findChildrenOfType(file, type));
    }

    public static <T extends PsiElement> T findElementByContainingText(PsiFile file, Class<T> type, String text) {
        for (T element : findElementsOfType(file, type)) {
            if (element.getText().contains(text)) {
                return element;
            }
        }
        Assert.fail("No " + type.getSimpleName() + " containing text: " + text);
        return null;
    }

    /**
     * Get the dependency tree child. Fail the test if it doesn't exist.
     *
     * @param depTree   - The dependency tree
     * @param childName - The child name to search
     * @return the dependency tree child.
     */
    public static DepTreeNode getAndAssertChild(DepTree depTree, DepTreeNode parent, String childName) {
        Assert.assertTrue(parent.getChildren().contains(childName));
        DepTreeNode childNode = depTree.nodes().get(childName);
        Assert.assertNotNull("Couldn't find node '" + childName + "'.", childNode);
        return childNode;
    }
}
