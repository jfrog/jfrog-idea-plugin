package com.jfrog.ide.idea.scan;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.npm.NpmTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.NpmInspection;
import com.jfrog.ide.idea.projects.NpmProject;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    private NpmTreeBuilder npmTreeBuilder;

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link FilterManagerService}, {@link LicensesTree} and {@link IssuesTree}.
     * @param project     - Npm project {@link NpmProject}.
     */
    NpmScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.NPM);
        getLog().info("Found npm project: " + getProjectName());
        npmTreeBuilder = new NpmTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap());
        subscribeLaunchDependencyScanOnFileChangedEvents("package-lock.json");
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) throws IOException {
        setScanResults(npmTreeBuilder.buildTree(getLog()));
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String packagjsonPath = Paths.get(Utils.getProjectBasePath(project).toString(), "package.json").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(packagjsonPath);
        PsiFile psiFile = PsiManager.getInstance(mainProject).findFile(file);
        return new PsiFile[] {psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new NpmInspection();
    }
}

