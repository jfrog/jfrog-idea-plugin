package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.yarn.YarnTreeBuilder;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.YarnInspection;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class YarnScanner extends SingleDescriptorScanner {

    private final YarnTreeBuilder yarnTreeBuilder;
    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath  the package.json directory
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    YarnScanner(Project project, String basePath, ExecutorService executor, ScanLogic scanLogic) {
        super(project, basePath, ComponentPrefix.NPM, executor, Paths.get(basePath, "package.json").toString(), scanLogic);
        getLog().info("Found yarn project: " + getProjectPath());
        yarnTreeBuilder = new YarnTreeBuilder(Paths.get(basePath), descriptorFilePath, EnvironmentUtil.getEnvironmentMap());
    }

    @Override
    protected DepTree buildTree() throws IOException {
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
        return new YarnInspection();
    }

    @Override
    protected PackageManagerType getPackageManagerType() {
        return PackageManagerType.YARN;
    }
}

