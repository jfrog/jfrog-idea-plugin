package com.jfrog.ide.idea.scan;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.gradle.GradleTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.GradleInspection;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private final GradleTreeBuilder gradleTreeBuilder;

    GradleScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.GAV);
        getLog().info("Found gradle project: " + getProjectName());
        gradleTreeBuilder = new GradleTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap());
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String buildGradlePath = Paths.get(Utils.getProjectBasePath(project).toString(), "build.gradle").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildGradlePath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new GradleInspection();
    }

    @Override
    protected void buildTree() throws IOException {
        setScanResults(gradleTreeBuilder.buildTree(getLog()));
    }
}