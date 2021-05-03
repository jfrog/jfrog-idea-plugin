package com.jfrog.ide.idea.scan;

import com.intellij.codeInspection.LocalInspectionTool;
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
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    private final NpmTreeBuilder npmTreeBuilder;

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param project     - Npm project {@link NpmProject}.
     */
    NpmScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.NPM);
        getLog().info("Found npm project: " + getProjectName());
        npmTreeBuilder = new NpmTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap());
        subscribeLaunchDependencyScanOnFileChangedEvents("package-lock.json");
    }

    @Override
    protected void buildTree() throws IOException {
        setScanResults(npmTreeBuilder.buildTree(getLog()));
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String packageJsonPath = Paths.get(Utils.getProjectBasePath(project).toString(), "package.json").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(packageJsonPath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(mainProject).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new NpmInspection();
    }
}

