package com.jfrog.ide.idea.inspections;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.scan.GradleScanManager;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public abstract class GradleInspection extends AbstractInspection {
    private int lastAnnotatedLine;

    public GradleInspection(String packageDescriptorName) {
        super(packageDescriptorName);
    }

    @Override
    ScanManager getScanManager(Project project, String path) {
        return ScanManagersFactory.getScanManagers(project).stream()
                .filter(GradleScanManager.class::isInstance)
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

    /**
     * Get all modules of the current project
     *
     * @param project - The current project
     * @return list of gradle modules or null if the Gradle project not yet initialized
     */
    private List<String> getGradleModules(Project project) {
        GradleSettings.MyState gradleState = GradleSettings.getInstance(project).getState();
        if (gradleState == null) {
            return null;
        }
        return gradleState.getLinkedExternalProjectsSettings().stream()
                .map(ExternalProjectSettings::getModules)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
