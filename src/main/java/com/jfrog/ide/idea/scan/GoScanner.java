package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.go.GoTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.GoInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.GoUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by Bar Belity on 06/02/2020.
 */
public class GoScanner extends SingleDescriptorScanner {
    private final GoTreeBuilder goTreeBuilder;
    private final String PKG_TYPE = "go";

    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath  the go.mod directory
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    GoScanner(Project project, String basePath, ExecutorService executor, ScanLogic scanLogic) {
        super(project, basePath, ComponentPrefix.GO, executor, Paths.get(basePath, "go.mod").toString(), scanLogic);
        getLog().info("Found Go project: " + getProjectPath());
        Map<String, String> env = Maps.newHashMap(EnvironmentUtil.getEnvironmentMap());
        String goExec = null;
        try {
            goExec = GoUtils.getGoExeAndSetEnv(env, project);
        } catch (NoClassDefFoundError error) {
            getLog().warn("Go plugin is not installed. Install it to get a better experience.");
        }
        goTreeBuilder = new GoTreeBuilder(goExec, Paths.get(basePath), descriptorFilePath, env, getLog());
    }

    @Override
    protected DepTree buildTree() throws IOException {
        return goTreeBuilder.buildTree();
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
        return new GoInspection();
    }

    @Override
    protected String getPackageManagerName() {
        return PKG_TYPE;
    }

    @Override
    public String getCodeBaseLanguage() {
        return "go";
    }
}
