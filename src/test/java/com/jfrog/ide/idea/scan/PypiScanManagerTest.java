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
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.idea.TestUtils.assertScopes;
import static com.jfrog.ide.idea.TestUtils.getAndAssertChild;

/**
 * @author yahavi
 **/
public class PypiScanManagerTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String SDK_NAME = "Test Python SDK";
    private static final String DIRECT_DEPENDENCY_NAME = "pipgrip";
    private static final String DIRECT_DEPENDENCY_VERSION = "0.6.8";
    private static final String TRANSITIVE_DEPENDENCY_NAME = "anytree";
    private static final String TRANSITIVE_DEPENDENCY_VERSION = "2.8.0";

    private ExecutorService executorService;
    private Sdk pythonSdk;
    private File tmpDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createVirtualEnv();
        resolvePythonSdk();
        installDependencyOnVirtualEnv();
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
        CommandResults results = commandExecutor.exeCommand(tmpDir, Lists.newArrayList("-m", "venv", "pip-venv"), null, new NullLog());
        if (!results.isOk()) {
            // The Python tests requires Python 3 because the "venv" module exists only at Python 3.
            // In some machines the "python" executable is Python 2.
            commandExecutor = new CommandExecutor("python3", null);
            results = commandExecutor.exeCommand(tmpDir, Lists.newArrayList("-m", "venv", "pip-venv"), null, new NullLog());
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

    public void testBuildTree() {
        PypiScanManager pypiScanManager = new PypiScanManager(getProject(), pythonSdk, executorService);
        pypiScanManager.setScanLogic(new GraphScanLogic(pypiScanManager.getPackageType(), new NullLog()));
        pypiScanManager.buildTree();

        // Check root SDK node
        DependencyTree results = pypiScanManager.buildTree();
        assertEquals(SDK_NAME, results.getUserObject());
        assertScopes(results);
        assertTrue(results.isMetadata());
        GeneralInfo generalInfo = results.getGeneralInfo();
        assertEquals("Python SDK", generalInfo.getPkgType());
        assertEquals(SDK_NAME, generalInfo.getArtifactId());
        assertEquals(pythonSdk.getHomePath(), generalInfo.getPath());
        assertNotEmpty(results.getChildren());

        // Check direct dependency
        DependencyTree pipGrip = getAndAssertChild(results, DIRECT_DEPENDENCY_NAME + ":" + DIRECT_DEPENDENCY_VERSION);
        assertFalse(pipGrip.isMetadata());
        assertSize(7, pipGrip.getChildren());
        generalInfo = pipGrip.getGeneralInfo();
        assertEquals("pypi", generalInfo.getPkgType());
        assertEquals(DIRECT_DEPENDENCY_NAME, generalInfo.getArtifactId());
        assertEquals(DIRECT_DEPENDENCY_VERSION, generalInfo.getVersion());

        // Check transitive dependency
        DependencyTree anyTree = getAndAssertChild(pipGrip, TRANSITIVE_DEPENDENCY_NAME + ":" + TRANSITIVE_DEPENDENCY_VERSION);
        assertFalse(anyTree.isMetadata());
        generalInfo = anyTree.getGeneralInfo();
        assertEquals("pypi", generalInfo.getPkgType());
        assertEquals(TRANSITIVE_DEPENDENCY_NAME, generalInfo.getArtifactId());
        assertEquals(TRANSITIVE_DEPENDENCY_VERSION, generalInfo.getVersion());
        assertSize(1, anyTree.getChildren());
    }
}
