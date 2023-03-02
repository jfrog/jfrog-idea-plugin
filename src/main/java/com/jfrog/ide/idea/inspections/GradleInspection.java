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

    public static String stripVersion(String componentId) {
        if (StringUtils.countMatches(componentId, ":") >= 2) {
            // implementation('a:b:c')
            componentId = componentId.split(":")[0] + ":" + componentId.split(":")[1];
        }
        return componentId;
    }
}
