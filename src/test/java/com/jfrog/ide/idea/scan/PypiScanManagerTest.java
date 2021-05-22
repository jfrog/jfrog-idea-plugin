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
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.packaging.PyPackageManagers;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.jfrog.ide.idea.TestUtils;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author yahavi
 **/
public class PypiScanManagerTest extends LightJavaCodeInsightFixtureTestCase {
    private static final String SDK_NAME = "Test Python SDK";
    private static final String DIRECT_DEPENDENCY_NAME = "pipgrip";
    private static final String DIRECT_DEPENDENCY_VERSION = "0.6.8";
    private static final String TRANSITIVE_DEPENDENCY_NAME = "anytree";
    private static final String TRANSITIVE_DEPENDENCY_VERSION = "2.8.0";

    private Sdk pythonSdk;
    private File tmpDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createVirtualEnv();
        resolvePythonSdk();
        installDependencyOnVirtualEnv();
    }

    @Override
    protected void tearDown() throws Exception {
        if (tmpDir != null) {
            FileUtils.deleteDirectory(tmpDir);
        }
        PyPackageManagers.getInstance().clearCache(pythonSdk);
        super.tearDown();
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return LightJavaCodeInsightFixtureTestCase.JAVA_11;
    }

    private void createVirtualEnv() throws IOException, InterruptedException {
        tmpDir = Files.createTempDirectory("").toFile();
        CommandExecutor commandExecutor = new CommandExecutor("python3", null);
        CommandResults results = commandExecutor.exeCommand(tmpDir, Lists.newArrayList("-m", "venv", "pip-venv"), null, new NullLog());
        assertTrue(results.getRes() + ". Error: " + results.getErr(), results.isOk());
    }

    private void resolvePythonSdk() {
        Path venvPath = tmpDir.toPath().resolve("pip-venv").resolve("bin").resolve("python");
        pythonSdk = new ProjectJdkImpl(SDK_NAME, PythonSdkType.getInstance(), venvPath.toString(), "");
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
        PypiScanManager pypiScanManager = new PypiScanManager(getProject());

        // Create Pypi dependency tree
        try (MockedStatic<PythonSdkUtil> mockController = Mockito.mockStatic(PythonSdkUtil.class)) {
            mockController.when(PythonSdkUtil::getAllSdks).thenReturn(Lists.newArrayList(pythonSdk));
            pypiScanManager.buildTree(null);
        }

        // Check root SDK node
        DependencyTree results = pypiScanManager.getScanResults();
        assertEquals(SDK_NAME, results.getUserObject());
        assertEquals(Sets.newHashSet(new Scope()), results.getScopes());
        GeneralInfo generalInfo = results.getGeneralInfo();
        assertEquals("Python SDK", generalInfo.getPkgType());
        assertEquals(SDK_NAME, generalInfo.getArtifactId());
        assertEquals(pythonSdk.getHomePath(), generalInfo.getPath());
        assertSize(1, results.getChildren());

        // Check direct dependency
        DependencyTree pipGrip = TestUtils.getAndAssertChild(results, DIRECT_DEPENDENCY_NAME + ":" + DIRECT_DEPENDENCY_VERSION);
        assertEquals(Sets.newHashSet(new Scope()), pipGrip.getScopes());
        assertSize(7, pipGrip.getChildren());
        generalInfo = pipGrip.getGeneralInfo();
        assertEquals("pypi", generalInfo.getPkgType());
        assertEquals(DIRECT_DEPENDENCY_NAME, generalInfo.getArtifactId());
        assertEquals(DIRECT_DEPENDENCY_VERSION, generalInfo.getVersion());

        // Check transitive dependency
        DependencyTree anyTree = TestUtils.getAndAssertChild(pipGrip, TRANSITIVE_DEPENDENCY_NAME + ":" + TRANSITIVE_DEPENDENCY_VERSION);
        assertEquals(Sets.newHashSet(new Scope()), anyTree.getScopes());
        generalInfo = anyTree.getGeneralInfo();
        assertEquals("pypi", generalInfo.getPkgType());
        assertEquals(TRANSITIVE_DEPENDENCY_NAME, generalInfo.getArtifactId());
        assertEquals(TRANSITIVE_DEPENDENCY_VERSION, generalInfo.getVersion());
        assertSize(1, anyTree.getChildren());
    }

    public void testRefreshPythonSdk() throws IOException {
        PypiScanManager pypiScanManager = new PypiScanManager(getProject());

        try (MockedStatic<PythonSdkUtil> mockController = Mockito.mockStatic(PythonSdkUtil.class)) {
            // Test 1 Python SDK
            List<Sdk> expected = Lists.newArrayList(pythonSdk);
            mockController.when(PythonSdkUtil::getAllSdks).thenReturn(expected);
            pypiScanManager.refreshPythonSdks();
            assertEquals(expected, pypiScanManager.getPythonSdks());
            assertTrue(PypiScanManager.isApplicable());

            // Test 2 Python SDKs
            expected = Lists.newArrayList(pythonSdk, new ProjectJdkImpl("Yet another Python SDK", PythonSdkType.getInstance()));
            mockController.when(PythonSdkUtil::getAllSdks).thenReturn(expected);
            pypiScanManager.refreshPythonSdks();
            assertEquals(expected, pypiScanManager.getPythonSdks());
            assertTrue(PypiScanManager.isApplicable());

            // Test no Python SDKs
            mockController.when(PythonSdkUtil::getAllSdks).thenReturn(Collections.emptyList());
            pypiScanManager.refreshPythonSdks();
            assertEmpty(pypiScanManager.getPythonSdks());
            assertFalse(PypiScanManager.isApplicable());
        }
    }
}
