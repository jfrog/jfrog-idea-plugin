package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.util.Collection;
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

    /**
     * Groups a collection of DependencyNodes by the descriptor files of the modules that depend on them.
     * The returned DependencyNodes inside the FileTreeNodes are references of the ones in depScanResults.
     *
     * @param depScanResults - collection of DependencyNodes.
     * @param depMap - a map of DependencyTree objects by their component ID.
     * @return A list of FileTreeNodes (that are all DescriptorFileTreeNodes) having the DependencyNodes as their children.
     */
    @Override
    protected List<FileTreeNode> groupDependenciesToDescriptorNodes(Collection<DependencyNode> depScanResults, Map<String, List<DependencyTree>> depMap) {
        DescriptorFileTreeNode fileTreeNode = new DescriptorFileTreeNode(descriptorFilePath);
        for (DependencyNode dependency : depScanResults) {
            boolean directDep = false;
            for (DependencyTree depTree : depMap.get(dependency.getComponentId())) {
                if (depTree.getParent() != null && depTree.getParent().getParent() == null) {
                    directDep = true;
                    break;
                }
            }
            dependency.setIndirect(!directDep);
            fileTreeNode.addDependency(dependency);
        }
        return List.of(fileTreeNode);
    }
}
