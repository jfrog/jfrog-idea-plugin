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
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.ConsistentFilterManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    private final NpmTreeBuilder npmTreeBuilder;
    private final String PKG_TYPE = "npm";

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath - The package.json directory.
     * @param executor - An executor that should limit the number of running tasks to 3
     */
    NpmScanManager(Project project, String basePath, ExecutorService executor) {
        super(project, basePath, ComponentPrefix.NPM, executor);
        getLog().info("Found npm project: " + getProjectName());
        npmTreeBuilder = new NpmTreeBuilder(Paths.get(basePath), EnvironmentUtil.getEnvironmentMap());
        subscribeLaunchDependencyScanOnFileChangedEvents("package-lock.json");
    }

    @Override
    protected void buildTree(boolean shouldToast) throws IOException {
        setScanResults(npmTreeBuilder.buildTree(getLog(), shouldToast));
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String packageJsonPath = Paths.get(basePath, "package.json").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(packageJsonPath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new NpmInspection();
    }

    @Override
    protected String getProjectPackageType() {
        return PKG_TYPE;
    }
}

