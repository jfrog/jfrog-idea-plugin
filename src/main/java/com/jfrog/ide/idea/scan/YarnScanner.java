package com.jfrog.ide.idea.scan;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.common.yarn.YarnTreeBuilder;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.YarnInspection;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.scan.utils.ImpactTreeBuilder;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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
        yarnTreeBuilder = new YarnTreeBuilder(Paths.get(basePath), descriptorFilePath, EnvironmentUtil.getEnvironmentMap(), getLog());
    }

    @Override
    protected DepTree buildTree() throws IOException {
        return yarnTreeBuilder.buildTree();
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

    /**
     * Builds a map of package name to versions out of a set of <package-name>:<version> Strings.
     *
     * @param packages - A set of packages in the format of 'package-name:version'.
     * @return - A map of package name to a set of versions.
     */
    Map<String, Set<String>> getPackageNameToVersionsMap(Set<String> packages) {
        Map<String, Set<String>> packageNameToVersions = new HashMap<>();
        for (String fullNamePackage : CollectionUtils.emptyIfNull(packages)) {
            String[] packageSplit = StringUtils.split(fullNamePackage, ":");
            if (packageSplit.length != 2) {
                this.getLog().error("Illegal package name: " + fullNamePackage + ". Skipping package, the dependency tree may be incomplete.");
                continue;
            }
            String packageName = packageSplit[0];
            String packageVersion = packageSplit[1];
            packageNameToVersions.putIfAbsent(packageName, new HashSet<>());
            packageNameToVersions.get(packageName).add(packageVersion);
        }
        return packageNameToVersions;
    }

    private void buildImpactGraphFromPaths(DescriptorFileTreeNode descriptorNode, Map<String, DependencyNode> vulnerableDependencies, Map<String, List<List<String>>> packageVersionsImpactPaths) {
        for (Map.Entry<String, List<List<String>>> aPackageVersionImpactPaths : packageVersionsImpactPaths.entrySet()) {
            String packageFullName = aPackageVersionImpactPaths.getKey();
            List<List<String>> impactPaths = aPackageVersionImpactPaths.getValue();
            DependencyNode dependencyNode = vulnerableDependencies.get(packageFullName);

            // build the impact graph for each vulnerable dependency out of its impact paths
            for (List<String> impactPath : impactPaths) {
                ImpactTreeBuilder.addImpactPathToDependencyNode(dependencyNode, impactPath);
            }

            boolean direct = impactPaths.stream().map(List::size).anyMatch(size -> size == 2);

            dependencyNode.setIndirect(!direct);
            descriptorNode.addDependency(dependencyNode);
        }
    }

    /**
     * Builds the impact graph for each given vulnerable dependencies.
     * The impact graph is built by running 'yarn why <package>' command, making it different from other package managers.
     *
     * @param vulnerableDependencies - The vulnerable dependencies to build the impact graph for.
     *                               The key is the package name and version, and the value is the dependency node.
     * @param depTree                - The whole dependency tree (not just vulnerable dependencies) that was generated earlier.
     * @return - The impact graph attached to package.json DescriptorFileTreeNode
     */
    @Override
    protected List<FileTreeNode> buildImpactGraph(Map<String, DependencyNode> vulnerableDependencies, DepTree depTree) throws IOException {
        DescriptorFileTreeNode descriptorNode = new DescriptorFileTreeNode(depTree.getRootNodeDescriptorFilePath());
        // Build a map of package name to versions, to avoid running 'yarn why' multiple times for the same package.
        Map<String, Set<String>> packageNameToVersions = this.getPackageNameToVersionsMap(vulnerableDependencies.keySet());

        for (Map.Entry<String, Set<String>> entry : packageNameToVersions.entrySet()) {
            // Find the impact paths for each package for all its vulnerable versions
            Map<String, List<List<String>>> packageVersionsImpactPaths = yarnTreeBuilder.findDependencyImpactPaths(depTree.rootId(), entry.getKey(), entry.getValue());
            // Build the impact graph for each vulnerable dependency out of its impact paths, set Indirect flag and add it to the descriptor node
            buildImpactGraphFromPaths(descriptorNode, vulnerableDependencies, packageVersionsImpactPaths);
        }

        // Return a list of one element - the descriptor node for package.json
        // COW list is used to avoid ConcurrentModificationException in SourceCodeScannerManager
        return new CopyOnWriteArrayList<>(Collections.singletonList(descriptorNode));
    }
}

