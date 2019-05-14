package com.jfrog.ide.idea.scan;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.npm.NpmTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    private NpmTreeBuilder npmTreeBuilder;

    NpmScanManager(Project project) throws IOException {
        super(project, ComponentPrefix.NPM);
        getLog().info("Found npm project: " + getProjectName());
        npmTreeBuilder = new NpmTreeBuilder(Utils.getProjectBasePath(project));
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies, @Nullable IdeModifiableModelsProvider modelsProvider) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) throws IOException {
        setScanResults(npmTreeBuilder.buildTree(getLog()));
    }

}

