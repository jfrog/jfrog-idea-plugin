package com.jfrog.ide.idea.inspections;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.intellij.lang.annotation.HighlightSeverity.INFORMATION;

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
     * @param elements         - The elements to apply the annotations
     * @param showIcon         - True if should add annotation icon
     */
    static void registerAnnotation(AnnotationHolder annotationHolder, DependencyTree dependency, PsiElement[] elements, boolean showIcon) {
        String licensesString = getLicensesString(dependency);
        String topIssue = getTopIssueString(dependency);
        AnnotationIconRenderer iconRenderer = showIcon ? new AnnotationIconRenderer(dependency, topIssue) : null;
        Arrays.stream(elements)
                .filter(Objects::nonNull)
                .forEach(element -> {
                    try {
                        AnnotationBuilder builder = annotationHolder.newAnnotation(INFORMATION, topIssue).range(element);
                        if (showIcon) {
                            iconRenderer.setProject(element.getProject());
                            builder = builder.gutterIconRenderer(iconRenderer);
                        }
                        builder.create();
                        annotationHolder.newAnnotation(INFORMATION, licensesString).range(element).create();
                    } catch (IllegalArgumentException e) {
                        // Exception is thrown when the element we register the annotation for is out of bound of the
                        // containing element exists in the provided annotationHolder.
                        // This scenario may occur during a gradle-inspections.
                    }
                });
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
