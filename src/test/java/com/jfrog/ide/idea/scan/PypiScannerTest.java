package com.jfrog.ide.idea.scan;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.ConcurrencyUtil;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageManagers;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.idea.TestUtils.getAndAssertChild;

/**
 * @author yahavi
 **/
public class PypiScannerTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String SDK_NAME = "Test Python SDK";
    private static final String DIRECT_DEPENDENCY_NAME = "pipgrip";
    private static final String DIRECT_DEPENDENCY_VERSION = "0.6.8";
    private static final String TRANSITIVE_DEPENDENCY_NAME = "anytree";

    private ExecutorService executorService;
    private Sdk pythonSdk;
    private File tmpDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createVirtualEnv();
        resolvePythonSdk();
        executorService = ConcurrencyUtil.newSameThreadExecutorService();
    }

    @Override
    protected void tearDown() throws Exception {
        if (tmpDir != null) {
            FileUtils.deleteDirectory(tmpDir);
        }
        PyPackageManagers.getInstance().clearCache(pythonSdk);
        executorService.shutdown();
        super.tearDown();
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_11;
    }

    private void createVirtualEnv() throws IOException, InterruptedException {
        tmpDir = Files.createTempDirectory("").toFile();
        CommandExecutor commandExecutor = new CommandExecutor("python", null);
        ArrayList<String> args = Lists.newArrayList("-m", "venv", "pip-venv");
        CommandResults results = commandExecutor.exeCommand(tmpDir, Lists.newArrayList(args), null, new NullLog());
        if (!results.isOk()) {
            // The Python tests requires Python 3 because the "venv" module exists only at Python 3.
            // In some machines the "python" executable is Python 2.
            commandExecutor = new CommandExecutor("python3", null);
            results = commandExecutor.exeCommand(tmpDir, Lists.newArrayList(args), null, new NullLog());
        }
        assertTrue(results.getRes() + ". Error: " + results.getErr(), results.isOk());
    }

    private void resolvePythonSdk() {
        Path virtualEnv = tmpDir.toPath().resolve("pip-venv");
        Path venvPath = SystemUtils.IS_OS_WINDOWS ? virtualEnv.resolve("Scripts") : virtualEnv.resolve("bin");
        pythonSdk = new ProjectJdkImpl(SDK_NAME, PythonSdkType.getInstance(), venvPath.resolve("python").toString(), "");
    }

    private void installDependencyOnVirtualEnv() {
        ProgressManager.getInstance().run(new Task.Modal(getProject(), "Install Dependency", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    PyPackageManager pyPackageManager = PyPackageManager.getInstance(pythonSdk);
                    pyPackageManager.install(DIRECT_DEPENDENCY_NAME + "==" + DIRECT_DEPENDENCY_VERSION);
                } catch (ExecutionException e) {
                    fail(ExceptionUtils.getRootCauseMessage(e));
                }
            }
        });
    }

    public void testBuildTree() throws IOException {
        System.out.println("Starting testBuildTree...");

        installDependencyOnVirtualEnv();
        System.out.println("Dependency installed on virtual environment.");

        PypiScanner pypiScanner = new PypiScanner(getProject(), pythonSdk, executorService, new GraphScanLogic(new NullLog()));
        System.out.println("PypiScanner initialized.");

        // Check root SDK node
        DepTree results = pypiScanner.buildTree();
        System.out.println("Dependency tree built: " + results);

        System.out.println("Root ID: " + results.rootId());
        assertEquals(SDK_NAME, results.rootId());
        assertEquals(SDK_NAME, results.rootId());
        System.out.println("Root descriptor file path: " + results.getRootNodeDescriptorFilePath());
        assertEquals(pythonSdk.getHomePath(), results.getRootNodeDescriptorFilePath());
        assertNotEmpty(results.getRootNode().getChildren());
        System.out.println("Root node children: " + results.getRootNode().getChildren());

        // Check direct dependency
        String directDepId = DIRECT_DEPENDENCY_NAME + ":" + DIRECT_DEPENDENCY_VERSION;
        System.out.println("Direct dependency ID: " + directDepId);
        DepTreeNode pipGrip = getAndAssertChild(results, results.getRootNode(), directDepId);
        System.out.println("Direct dependency node: " + pipGrip);
        assertSize(7, pipGrip.getChildren());
        System.out.println("Direct dependency children: " + pipGrip.getChildren().toString());

        // Check transitive dependency
        String anyTreeDepId = pipGrip.getChildren().stream()
                .filter(childId -> childId.startsWith(TRANSITIVE_DEPENDENCY_NAME + ":"))
                .findFirst()
                .orElse(null);
        System.out.println("Transitive dependency ID: " + anyTreeDepId);
        assertNotNull("Couldn't find node '" + anyTreeDepId + "'.", anyTreeDepId);

        DepTreeNode anyTreeDepNode = results.nodes().get(anyTreeDepId);
        System.out.println("Transitive dependency node: " + anyTreeDepNode);
        assertSize(1, anyTreeDepNode.getChildren());
        System.out.println("Transitive dependency children size: " + anyTreeDepNode.getChildren().size());
    }

    public void testBuildTreeCircularDependency() throws IOException {
        try (MockedStatic<PyPackageManager> mockController = Mockito.mockStatic(PyPackageManager.class)) {
            mockController.when(() -> PyPackageManager.getInstance(pythonSdk)).thenReturn(new DummyCircularDepPyPkgManager(pythonSdk));
            PypiScanner pypiScanner = new PypiScanner(getProject(), pythonSdk, executorService, new GraphScanLogic(new NullLog()));

            // Check root SDK node
            DepTree results = pypiScanner.buildTree();
            assertEquals(SDK_NAME, results.rootId());
            assertEquals(pythonSdk.getHomePath(), results.getRootNodeDescriptorFilePath());
            assertNotEmpty(results.getRootNode().getChildren());

            // The expected tree: root -> a-> b -> a
            // Check root
            String directDepId = DummyCircularDepPyPkgManager.DIRECT_DEPENDENCY_NAME + ":" + DummyCircularDepPyPkgManager.DIRECT_DEPENDENCY_VERSION;
            DepTreeNode root = getAndAssertChild(results, results.getRootNode(), directDepId);
            assertSize(1, root.getChildren());
            String depIdA = DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_A + ":" + DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_VERSION;
            DepTreeNode a = getAndAssertChild(results, root, depIdA);

            // Check dependency "a"
            assertSize(1, a.getChildren());
            String depIdB = DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_B + ":" + DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_VERSION;
            DepTreeNode b = getAndAssertChild(results, a, depIdB);

            // Check dependency "b"
            assertSize(1, b.getChildren());
            getAndAssertChild(results, b, DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_A + ":" + DummyCircularDepPyPkgManager.CIRCULAR_DEPENDENCY_VERSION);
        }
    }
}
