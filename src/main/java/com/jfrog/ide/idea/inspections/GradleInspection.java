package com.jfrog.ide.idea.inspections;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.scan.GradleScanManager;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public abstract class GradleInspection extends AbstractInspection {

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
    Set<DependencyTree> getModules(PsiElement element, GeneralInfo generalInfo) {
        Project project = element.getProject();
        DependencyTree root = getRootDependencyTree(element);
        List<String> gradleModules = getGradleModules(project);
        if (root == null || gradleModules == null) {
            return null;
        }

        // Collect the modules containing the dependency
        return collectModules(root, project, gradleModules, generalInfo);
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
