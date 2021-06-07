package com.jfrog.ide.idea.scan;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.go.GoTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.GoInspection;
import com.jfrog.ide.idea.projects.GoProject;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Bar Belity on 06/02/2020.
 */
public class GoScanManager extends ScanManager {

    private final GoTreeBuilder goTreeBuilder;

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param project     - Go project {@link GoProject}.
     */
    GoScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.GO);
        getLog().info("Found go project: " + getProjectName());
        goTreeBuilder = new GoTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap(), getLog());
        subscribeLaunchDependencyScanOnFileChangedEvents("go.sum");
    }

    @Override
    protected void buildTree() throws IOException {
        setScanResults(goTreeBuilder.buildTree());
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String goModPath = Paths.get(Utils.getProjectBasePath(project).toString(), "go.mod").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(goModPath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(mainProject).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new GoInspection();
    }
}
