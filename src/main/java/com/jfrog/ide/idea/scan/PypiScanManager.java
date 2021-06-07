package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.packaging.PyPackage;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageUtil;
import com.jetbrains.python.packaging.PyRequirement;
import com.jetbrains.python.packaging.pipenv.PyPipEnvPackageManager;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public class PypiScanManager extends ScanManager {
    private List<Sdk> pythonSdks = Lists.newArrayList();
    private final Disposable sdkDisposable = () -> {
    };

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     */
    PypiScanManager(Project mainProject) throws IOException {
        super(mainProject, mainProject, ComponentPrefix.PYPI);
        getLog().info("Found PyPI project: " + getProjectName());
    }

    static boolean isApplicable() {
        return CollectionUtils.isNotEmpty(PythonSdkUtil.getAllSdks());
    }

    List<Sdk> getPythonSdks() {
        return pythonSdks;
    }

    @Override
    protected void buildTree() {
        refreshPythonSdks();
        DependencyTree rootNode = createRootNode();
        initDependencyNode(rootNode, project.getName(), "", Utils.getProjectBasePath(project).toString(), "pypi");

        for (Sdk pythonSdk : pythonSdks) {
            try {
                DependencyTree sdkNode = createSdkDependencyTree(pythonSdk);
                rootNode.add(sdkNode);
            } catch (ExecutionException e) {
                getLog().error(ExceptionUtils.getRootCauseMessage(e), e);
            }
        }
        if (rootNode.getChildren().size() == 1) {
            setScanResults((DependencyTree) rootNode.getChildAt(0));
        } else {
            setScanResults(rootNode);
        }
    }

    /**
     * Refresh Python SDKs and listen to changes in the SDK environment.
     * For example, a scan should be triggered after running 'pip install'.
     */
    void refreshPythonSdks() {
        if (!Disposer.isDisposed(sdkDisposable)) {
            // Remove old "RunOnChangeUnderInterpreterPaths" listeners
            Disposer.dispose(sdkDisposable);
        }
        Disposer.register(project, sdkDisposable);
        List<Sdk> pythonSdks = PythonSdkUtil.getAllSdks();
        for (Sdk pythonSdk : pythonSdks) {
            getLog().debug("Found Python SDK: " + pythonSdk.getName());
            PyPackageUtil.runOnChangeUnderInterpreterPaths(pythonSdk, sdkDisposable, this::asyncScanAndUpdateResults);
        }
        this.pythonSdks = pythonSdks;
    }

    /**
     * Create a root node of all Python SDKs.
     *
     * @return root node of all Python SDKs.
     */
    private DependencyTree createRootNode() {
        DependencyTree rootNode = new DependencyTree(project.getName());
        GeneralInfo generalInfo = new GeneralInfo().artifactId(project.getName()).path(Utils.getProjectBasePath(project).toString()).pkgType("pypi");
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
            // If pyPackage contained in one of the dependencies, we conclude it is transitive dependency.
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
        GeneralInfo generalInfo = new GeneralInfo()
                .artifactId(name)
                .version(version)
                .path(path)
                .pkgType(type);
        node.setGeneralInfo(generalInfo);
        node.setScopes(Sets.newHashSet(new Scope()));
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        return null;
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return null;
    }
}
