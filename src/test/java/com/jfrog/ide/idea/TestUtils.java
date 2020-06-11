package com.jfrog.ide.idea;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.junit.Assert;

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
}
