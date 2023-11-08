package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public abstract class SingleDescriptorScanner extends ScannerBase {
    protected String descriptorFilePath;

    /**
     * @param project            currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                           like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath           project base path
     * @param prefix             components prefix for xray scan, e.g. gav:// or npm://
     * @param executor           an executor that should limit the number of running tasks to 3
     * @param descriptorFilePath path to the project's descriptor file
     * @param scanLogic          the scan logic to use
     */
    SingleDescriptorScanner(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor,
                            String descriptorFilePath, ScanLogic scanLogic) {
        super(project, basePath, prefix, executor, scanLogic);
        this.descriptorFilePath = descriptorFilePath;
    }

    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath  project base path
     * @param prefix    components prefix for xray scan, e.g. gav:// or npm://
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    SingleDescriptorScanner(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor,
                            ScanLogic scanLogic) {
        this(project, basePath, prefix, executor, "", scanLogic);
    }

    protected List<FileTreeNode> walkDepTree(Map<String, DependencyNode> vulnerableDependencies, DepTree depTree) throws IOException {
        return super.walkDepTree(vulnerableDependencies, depTree);
    }
}
