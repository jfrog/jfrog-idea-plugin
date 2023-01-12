package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.yarn.YarnTreeBuilder;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.NpmInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class YarnScanManager extends SingleDescriptorScanManager {

    private final YarnTreeBuilder yarnTreeBuilder;
    private final String PKG_TYPE = "yarn";

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath - The package.json directory.
     * @param executor - An executor that should limit the number of running tasks to 3
     */
    YarnScanManager(Project project, String basePath, ExecutorService executor) {
        super(project, basePath, ComponentPrefix.NPM, executor, Paths.get(basePath, "package.json").toString());
        getLog().info("Found yarn project: " + getProjectName());
        yarnTreeBuilder = new YarnTreeBuilder(Paths.get(basePath), EnvironmentUtil.getEnvironmentMap());
    }

    @Override
    protected DependencyTree buildTree() throws IOException {
        return yarnTreeBuilder.buildTree(getLog());
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(descriptorFilePath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected AbstractInspection getInspectionTool() {
        return new NpmInspection();
    }

    @Override
    protected String getPackageManagerName() {
        return PKG_TYPE;
    }

    @Override
    public String getPackageType() {
        return "npm";
    }
}

