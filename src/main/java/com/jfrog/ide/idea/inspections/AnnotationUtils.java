package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public class AnnotationUtils {

    private final static Issue NORMAL_SEVERITY_ISSUE = new Issue();

    /**
     * Register "Top issue" and "Licenses" annotations.
     *
     * @param annotationHolder - The annotations will be registered in this container
     * @param dependency       - The dependency tree node correlated to the element
     * @param elements         - The elements to apply the annotations.
     */
    static void registerAnnotation(AnnotationHolder annotationHolder, DependencyTree dependency, PsiElement[] elements) {
        HighlightSeverity problemHighlightType = getHighlightSeverity(dependency);
        String licensesString = getLicensesString(dependency);
        String topIssue = getTopIssueString(dependency);
        Arrays.stream(elements)
                .filter(Objects::nonNull)
                .forEach(element -> {
                    try {
                        annotationHolder.newAnnotation(problemHighlightType, topIssue).range(element).create();
                        annotationHolder.newAnnotation(problemHighlightType, licensesString).range(element).create();
                    } catch (IllegalArgumentException e) {
                        // Exception is thrown when the element we register the annotation for is out of bound of the
                        // containing element exists in the provided annotationHolder.
                        // This scenario may occur during a gradle-inspections.
                    }
                });
    }

    /**
     * Get the severity of the dependency tree node.
     *
     * @param node - The dependency tree node
     * @return the severity of the dependency tree node
     */
    private static HighlightSeverity getHighlightSeverity(DependencyTree node) {
        switch (node.getTopIssue().getSeverity()) {
            case High:
            case Critical:
                return HighlightSeverity.ERROR; // Red underline
            case Low:
            case Medium:
                return HighlightSeverity.WEAK_WARNING; // White underline
            default: // Normal, information, unknown and pending
                return HighlightSeverity.INFORMATION; // No underline
        }
    }

    /**
     * Get the top issue string.
     *
     * @param node - The dependency tree node
     * @return the top issue string
     */
    private static String getTopIssueString(DependencyTree node) {
        Issue topIssue = node.getTopIssue();
        if (topIssue.isHigherSeverityThan(NORMAL_SEVERITY_ISSUE)) {
            return "Top issue severity: " + topIssue.getSeverity();
        }
        return "No issues found";
    }

    /**
     * Get licenses string
     *
     * @param node - The dependency tree node
     * @return licenses string
     */
    private static String getLicensesString(DependencyTree node) {
        String results = "Licenses: ";
        List<String> licensesStrings = node.getLicenses().stream().map(License::getName).collect(Collectors.toList());
        if (licensesStrings.isEmpty()) {
            return results + "Unknown";
        }
        return results + String.join(", ", licensesStrings);
    }
}
