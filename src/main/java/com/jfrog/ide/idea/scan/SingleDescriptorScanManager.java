package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.DescriptorFileTreeNode;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

public abstract class SingleDescriptorScanManager extends ScanManager {
    protected String descriptorFilePath;

    /**
     * @param project            - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                           like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath           - Project base path
     * @param prefix             - Components prefix for xray scan, e.g. gav:// or npm://
     * @param executor           - An executor that should limit the number of running tasks to 3
     * @param descriptorFilePath - Path to the project's descriptor file
     */
    SingleDescriptorScanManager(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor, String descriptorFilePath) {
        super(project, basePath, prefix, executor);
        this.descriptorFilePath = descriptorFilePath;
    }

    /**
     * @param project            - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                           like {@link ConsistentFilterManager} and {@link ComponentsTree}
     * @param basePath           - Project base path
     * @param prefix             - Components prefix for xray scan, e.g. gav:// or npm://
     * @param executor           - An executor that should limit the number of running tasks to 3
     */
    SingleDescriptorScanManager(@NotNull Project project, String basePath, ComponentPrefix prefix, ExecutorService executor) {
        this(project, basePath, prefix, executor, "");
    }

    protected List<FileTreeNode> groupArtifactsToDescriptorNodes(Collection<Artifact> depScanResults) {
        DescriptorFileTreeNode fileTreeNode = new DescriptorFileTreeNode(descriptorFilePath);
        fileTreeNode.addDependencies(depScanResults);
        return Arrays.asList(fileTreeNode);
    }
}
