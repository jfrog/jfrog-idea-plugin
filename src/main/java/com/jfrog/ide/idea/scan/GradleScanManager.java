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
import com.jfrog.ide.idea.inspections.GradleGroovyInspection;
import com.jfrog.ide.idea.inspections.GradleKotlinInspection;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private final GradleTreeBuilder gradleTreeBuilder;
    private boolean isKotlin;

    GradleScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.GAV);
        getLog().info("Found gradle project: " + getProjectName());
        gradleTreeBuilder = new GradleTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap());
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        Path basePath = Utils.getProjectBasePath(project);
        VirtualFile file = localFileSystem.findFileByPath(basePath.resolve("build.gradle").toString());
        if (file == null) {
            file = localFileSystem.findFileByPath(basePath.resolve("build.gradle.kts").toString());
            if (file == null) {
                return null;
            }
            isKotlin = true;
        }
        PsiFile psiFile = PsiManager.getInstance(mainProject).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return isKotlin ? new GradleKotlinInspection() : new GradleGroovyInspection();
    }

    @Override
    protected void buildTree() throws IOException {
        setScanResults(gradleTreeBuilder.buildTree(getLog()));
    }
}