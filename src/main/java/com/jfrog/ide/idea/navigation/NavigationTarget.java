package com.jfrog.ide.idea.navigation;

import com.intellij.psi.PsiElement;

import java.util.Objects;

/**
 * Created by Bar Belity on 14/05/2020.
 */
public class NavigationTarget {

    private PsiElement element;
    private int lineNumber;

    NavigationTarget(PsiElement element, int lineNumber) {
        this.element = element;
        this.lineNumber = lineNumber;
    }

    public PsiElement getElement() {
        return element;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavigationTarget)) return false;
        NavigationTarget that = (NavigationTarget) o;
        return lineNumber == that.lineNumber &&
                Objects.equals(element.getContainingFile(), that.element.getContainingFile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(element.getContainingFile(), lineNumber);
    }
}
