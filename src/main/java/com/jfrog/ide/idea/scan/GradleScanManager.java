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
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.GradleGroovyInspection;
import com.jfrog.ide.idea.inspections.GradleKotlinInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.ConsistentFilterManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private final GradleTreeBuilder gradleTreeBuilder;
    private boolean kotlin;

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath - The build.gradle or build.gradle.kts directory.
     */
    GradleScanManager(Project project, String basePath, ScanLogic logic) throws IOException {
        super(project, basePath, ComponentPrefix.GAV, logic);
        getLog().info("Found Gradle project: " + getProjectName());
        gradleTreeBuilder = new GradleTreeBuilder(Paths.get(basePath), EnvironmentUtil.getEnvironmentMap());
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        Path basePath = Paths.get(this.basePath);
        VirtualFile file = localFileSystem.findFileByPath(basePath.resolve("build.gradle").toString());
        if (file == null) {
            file = localFileSystem.findFileByPath(basePath.resolve("build.gradle.kts").toString());
            if (file == null) {
                return null;
            }
            kotlin = true;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return kotlin ? new GradleKotlinInspection() : new GradleGroovyInspection();
    }

    @Override
    protected void buildTree(boolean shouldToast) throws IOException {
        setScanResults(gradleTreeBuilder.buildTree(getLog()));
    }
}