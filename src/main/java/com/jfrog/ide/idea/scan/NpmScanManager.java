package com.jfrog.ide.idea.scan;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.npm.NpmTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.NpmProject;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    private Path projectBasePath;

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link FilterManagerService}, {@link LicensesTree} and {@link IssuesTree}.
     * @param project     - Npm project {@link NpmProject}.
     */
    NpmScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.NPM);
        getLog().info("Found npm project: " + getProjectName());
        projectBasePath = Utils.getProjectBasePath(project);
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies, @Nullable IdeModifiableModelsProvider modelsProvider) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) throws IOException {
        String npmExecutablePath = "";
        String nodeJsInterpreterPath = PropertiesComponent.getInstance(mainProject).getValue("nodejs_interpreter_path");
        if (StringUtils.isNotBlank(nodeJsInterpreterPath)) {
            Path nodeJsPath = Paths.get(nodeJsInterpreterPath);
            npmExecutablePath = nodeJsPath.resolveSibling("npm").toAbsolutePath().toString();
        }
        NpmTreeBuilder npmTreeBuilder = new NpmTreeBuilder(projectBasePath, npmExecutablePath);
        setScanResults(npmTreeBuilder.buildTree(getLog()));
    }

}

