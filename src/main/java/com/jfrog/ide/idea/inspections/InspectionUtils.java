package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.common.tree.DependencyNode;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author yahavi
 */
public class InspectionUtils {

    final static String INSPECTION_MESSAGE = "Show in JFrog plugin";

    /**
     * Create the 'Show in JFrog plugin' quickfix.
     *
     * @param problemsHolder - The "Show in JFrog plugin" quickfix will be registered in this container
     * @param dependency     - The dependency tree node correlated to the element
     * @param elements       - The elements to apply the annotations
     */
    static void registerProblem(ProblemsHolder problemsHolder, DependencyNode dependency, PsiElement[] elements, int dependenciesSize) {
        String description = getDescription(dependency, dependenciesSize);
        ShowInDependencyTree quickFix = new ShowInDependencyTree(dependency, description);
        Arrays.stream(elements)
                .filter(Objects::nonNull)
                .forEach(element -> problemsHolder.registerProblem(element, description, ProblemHighlightType.INFORMATION, quickFix));
    }

    /**
     * Get the description of the Show in JFrog plugin quickfix.
     *
     * @param dependency       - The dependency tree node
     * @param dependenciesSize - The size of the dependency tree list
     * @return the description of the Show in JFrog plugin quickfix
     */
    private static String getDescription(DependencyNode dependency, int dependenciesSize) {
        if (dependenciesSize > 1) {
            return INSPECTION_MESSAGE + " ("+ dependency.getTitle() + ")";
        }
        return INSPECTION_MESSAGE;
    }
}
