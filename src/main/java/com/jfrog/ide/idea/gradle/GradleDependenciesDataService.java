package com.jfrog.ide.idea.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
@Order(ExternalSystemConstants.UNORDERED)
public class GradleDependenciesDataService extends AbstractProjectDataService<ProjectDependencies, Module> {

    @NotNull
    @Override
    public Key<ProjectDependencies> getTargetDataKey() {
        return ProjectKeys.DEPENDENCIES_GRAPH;
    }

    /**
     * This function is called after change in the build.gradle file or refresh gradle dependencies call.
     *
     * @param toImport       the project dependencies
     * @param projectData    the project data
     * @param project        the current project
     * @param modelsProvider contains the project modules
     */
    @Override
    public void importData(@NotNull Collection<DataNode<ProjectDependencies>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        if (GlobalSettings.getInstance().areXrayCredentialsSet()) {
            ScanManagersFactory.getInstance(project).tryScanSingleProject(project, toImport);
        }
    }
}
