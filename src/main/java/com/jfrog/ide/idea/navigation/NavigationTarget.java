package com.jfrog.ide.idea.navigation;

import com.intellij.psi.PsiElement;

import java.util.Objects;

/**
 * Created by Bar Belity on 14/05/2020.
 */
public class NavigationTarget {

    private final PsiElement element;
    private final int lineNumber;

    private final String componentName;

    NavigationTarget(PsiElement element, int lineNumber, String componentName) {
        this.element = element;
        this.lineNumber = lineNumber;
        this.componentName = componentName;
    }

    public PsiElement getElement() {
        return element;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getComponentName() {
        return componentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavigationTarget)) return false;
        NavigationTarget that = (NavigationTarget) o;
        return lineNumber == that.lineNumber &&
                element.isValid() && that.element.isValid() &&
                Objects.equals(element.getContainingFile(), that.element.getContainingFile());
    }

    @Override
    public int hashCode() {
        return Objects.hash(element.getContainingFile(), lineNumber);
    }
}
