package com.jfrog.ide.idea.inspections;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.scan.GradleScanner;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScannerBase;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yahavi
 */
public abstract class GradleInspection extends AbstractInspection {
    private int lastAnnotatedLine;

    public GradleInspection(String packageDescriptorName) {
        super(packageDescriptorName);
    }

    @Override
    ScannerBase getScanner(Project project, String path) {
        return ScanManager.getScanners(project).stream()
                .filter(GradleScanner.class::isInstance)
                .findAny()
                .orElse(null);
    }


    @Override
    boolean showAnnotationIcon(PsiElement element) {
        Document document = element.getContainingFile().getViewProvider().getDocument();
        boolean showAnnotationIcon = true;
        if (document != null) {
            int currentLine = document.getLineNumber(element.getTextOffset());
            showAnnotationIcon = currentLine != lastAnnotatedLine;
            lastAnnotatedLine = currentLine;
        }
        return showAnnotationIcon;
    }

    /**
     * Create component name from component ID in build.gradle or build.gradle.kts files.
     * Some examples:
     * compile project(':xyz') > xyz
     * implementation('a:b:c') > a:b
     * implementation('a:b') > a:b
     *
     * @param componentId - Component ID from the build.gradle or build.gradle.kts files
     * @return component name.
     */
    String createComponentName(String componentId) {
        if (StringUtils.countMatches(componentId, ":") == 2) {
            // implementation('a:b:c')
            return StringUtils.substringBeforeLast(componentId, ":");
        }
        // compile project(':xyz')
        return StringUtils.removeStart(componentId, ":");
    }
}
