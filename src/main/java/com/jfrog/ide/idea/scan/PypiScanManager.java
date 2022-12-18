package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageUtil;
import com.jetbrains.python.packaging.PyRequirement;
import com.jetbrains.python.packaging.pipenv.PyPipEnvPackageManager;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * @author yahavi
 */
public class PypiScanManager extends SingleDescriptorScanManager {
    private final Sdk pythonSdk;
    private final String PKG_TYPE = "pypi";

    static List<Sdk> getAllPythonSdks() {
        return PythonSdkUtil.getAllSdks();
    }

    /**
     * @param project   - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param pythonSdk - The Python SDK
     * @param executor  - An executor that should limit the number of running tasks to 3
     */
    PypiScanManager(Project project, Sdk pythonSdk, ExecutorService executor) {
        super(project, pythonSdk.getHomePath(), ComponentPrefix.PYPI, executor, pythonSdk.getHomePath());
        this.pythonSdk = pythonSdk;
        getLog().info("Found PyPI SDK: " + getProjectName());
        // TODO: can be removed?
//        PyPackageUtil.runOnChangeUnderInterpreterPaths(pythonSdk, this, this::asyncScanAndUpdateResults);
    }

    @Override
    protected DependencyTree buildTree(boolean shouldToast) {
        DependencyTree rootNode = createRootNode();
        initDependencyNode(rootNode, pythonSdk.getName(), "", pythonSdk.getHomePath(), "pypi");

        try {
            rootNode.add(createSdkDependencyTree(pythonSdk));
        } catch (ExecutionException e) {
            logError(getLog(), "", e, shouldToast);
        }
        if (rootNode.getChildren().size() == 1) {
            rootNode = (DependencyTree) rootNode.getChildAt(0);
        }
        return rootNode;
    }

    /**
     * Create a root node of all Python SDKs.
     *
     * @return root node of all Python SDKs.
     */
    private DependencyTree createRootNode() {
        DependencyTree rootNode = new DependencyTree(pythonSdk.getName());
        rootNode.setMetadata(true);
        GeneralInfo generalInfo = new GeneralInfo().componentId(pythonSdk.getName()).path(pythonSdk.getHomePath()).pkgType("pypi");
        rootNode.setGeneralInfo(generalInfo);
        rootNode.setScopes(Sets.newHashSet(new Scope()));
        return rootNode;
    }

    /**
     * Create a dependency tree for a given Python SDK.
     *
     * @param pythonSdk - The python SDK
     * @return dependency tree created for a given Python SDK.
     */
    private DependencyTree createSdkDependencyTree(Sdk pythonSdk) throws ExecutionException {
        // Retrieve all Pypi packages
        PyPackageManager packageManager = PyPipEnvPackageManager.getInstance(pythonSdk);
        List<PyPackage> packages = packageManager.refreshAndGetPackages(true);
        getLog().debug(CollectionUtils.size(packages) + " Pypi packages found in SDK " + pythonSdk.getName());

        // Create root SDK node
        DependencyTree sdkNode = new DependencyTree(pythonSdk.getName());
        sdkNode.setMetadata(true);
        initDependencyNode(sdkNode, pythonSdk.getName(), pythonSdk.getVersionString(), pythonSdk.getHomePath(), "Python SDK");

        // Create dependency mapping
        Map<String, PyPackage> dependencyMapping = new HashMap<>();
        for (PyPackage pyPackage : packages) {
            dependencyMapping.put(pyPackage.getName().toLowerCase(), pyPackage);
        }

        // Populate dependency tree
        Collection<PyPackage> values = dependencyMapping.values();
        Set<String> allDependencies = values.parallelStream()
                .map(PyPackage::getRequirements)
                .flatMap(Collection::stream)
                .map(PyRequirement::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        for (PyPackage pyPackage : values) {
            // If pyPackage is contained in one of the dependencies, we conclude it is a transitive dependency.
            // If it's transitive, we shouldn't add it as a direct dependency.
            if (!allDependencies.contains(pyPackage.getName().toLowerCase())) {
                populateDependencyTree(sdkNode, pyPackage, dependencyMapping);
            }
        }

        return sdkNode;
    }

    /**
     * Recursively, populate the SDK dependency tree.
     *
     * @param node              - Dependency tree node
     * @param pyPackage         - Current Python package
     * @param dependencyMapping - dependency name to Python package mapping
     */
    void populateDependencyTree(DependencyTree node, PyPackage pyPackage, Map<String, PyPackage> dependencyMapping) {
        String unresolved = pyPackage.isInstalled() ? "" : " [Unresolved]";
        DependencyTree child = new DependencyTree(pyPackage.getName() + ":" + pyPackage.getVersion() + unresolved);
        initDependencyNode(child, pyPackage.getName(), pyPackage.getVersion(), "", "pypi");
        node.add(child);

        for (PyRequirement requirement : pyPackage.getRequirements()) {
            PyPackage dependency = dependencyMapping.get(requirement.getName().toLowerCase());
            if (dependency == null) {
                getLog().warn("Dependency " + requirement.getName() + " is not installed.");
                continue;
            }
            populateDependencyTree(child, dependency, dependencyMapping);
        }
    }

    /**
     * Set general info and the 'None' scope to a dependency tree node.
     *
     * @param node    - The dependency tree to init
     * @param name    - Dependency name
     * @param version - Dependency version
     * @param path    - Path to project/sdk if applicable
     * @param type    - Dependency type
     */
    private void initDependencyNode(DependencyTree node, String name, String version, String path, String type) {
        GeneralInfo generalInfo = new GeneralInfo().path(path).pkgType(type).componentId(createComponentId(name, version));
        node.setGeneralInfo(generalInfo);
        node.setScopes(Sets.newHashSet(new Scope()));
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
    protected String getProjectPackageType() {
        return PKG_TYPE;
    }
}
