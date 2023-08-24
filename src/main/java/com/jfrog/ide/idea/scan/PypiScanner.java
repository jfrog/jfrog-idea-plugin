package com.jfrog.ide.idea.scan;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyRequirement;
import com.jetbrains.python.packaging.pipenv.PyPipEnvPackageManager;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * @author yahavi
 */
public class PypiScanner extends SingleDescriptorScanner {
    private final Sdk pythonSdk;

    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param pythonSdk the Python SDK
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    PypiScanner(Project project, Sdk pythonSdk, ExecutorService executor, ScanLogic scanLogic) {
        super(project, pythonSdk.getHomePath(), ComponentPrefix.PYPI, executor, pythonSdk.getHomePath(), scanLogic);
        this.pythonSdk = pythonSdk;
        getLog().info("Found PyPI SDK: " + getProjectPath());
    }

    @Override
    protected DepTree buildTree() throws IOException {
        try {
            return createSdkDependencyTree(pythonSdk);
        } catch (ExecutionException e) {
            throw new IOException(e);
        }
    }

    /**
     * Create a dependency tree for a given Python SDK.
     *
     * @param pythonSdk the Python SDK
     * @return dependency tree created for a given Python SDK.
     */
    private DepTree createSdkDependencyTree(Sdk pythonSdk) throws ExecutionException {
        // Retrieve all PyPI packages
        PyPackageManager packageManager = PyPipEnvPackageManager.getInstance(pythonSdk);
        List<PyPackage> packages = packageManager.refreshAndGetPackages(true);
        getLog().debug(CollectionUtils.size(packages) + " PyPI packages found in SDK " + pythonSdk.getName());

        // Create dependency mapping
        Map<String, String> compIdByCompName = new HashMap<>();
        // A set of the direct dependencies IDs. We add the IDs of all packages to the set, and remove the transitive ones below.
        Set<String> directDeps = new HashSet<>();
        for (PyPackage pyPackage : packages) {
            String compId = createComponentId(pyPackage.getName(), pyPackage.getVersion());
            compIdByCompName.put(pyPackage.getName().toLowerCase(), compId);
            directDeps.add(compId);
        }

        // Populate each node's children
        Map<String, DepTreeNode> nodes = new HashMap<>();
        for (PyPackage pyPackage : packages) {
            String compId = createComponentId(pyPackage.getName(), pyPackage.getVersion());
            DepTreeNode node = new DepTreeNode();
            for (PyRequirement requirement : pyPackage.getRequirements()) {
                String depId = compIdByCompName.get(requirement.getName().toLowerCase());
                if (depId == null) {
                    getLog().warn("Dependency " + requirement.getName() + " is not installed.");
                    continue;
                }
                node.getChildren().add(depId);
            }
            nodes.put(compId, node);
        }

        // Create root SDK node
        String rootCompId = pythonSdk.getName();
        DepTreeNode sdkNode = new DepTreeNode().descriptorFilePath(pythonSdk.getHomePath());
        sdkNode.children(directDeps);
        nodes.put(rootCompId, sdkNode);
        return new DepTree(rootCompId, nodes);
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        return null;
    }

    @Override
    protected AbstractInspection getInspectionTool() {
        return null;
    }

    @Override
    protected PackageManagerType getPackageManagerType() {
        return PackageManagerType.PYPI;
    }
}
