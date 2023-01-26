package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ConcurrencyUtil;
import com.jfrog.ide.common.gradle.GradleDriver;
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.idea.TestUtils.assertScopes;
import static com.jfrog.ide.idea.TestUtils.getAndAssertChild;

/**
 * @author yahavi
 **/
public class GradleScannerTest extends HeavyPlatformTestCase {
    private static final Path GRADLE_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "gradle"));
    private ExecutorService executorService;
    private String wrapperProjectDir;
    private String globalProjectDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wrapperProjectDir = createTempProjectDir("wrapper");

        // This is intended to make sure the Gradle Wrapper distribution was downloaded to the machine
        GradleDriver driver = new GradleDriver(Path.of(wrapperProjectDir, SystemUtils.IS_OS_WINDOWS ? "gradlew.bat" : "gradlew").toString(), System.getenv());
        driver.verifyGradleInstalled();

        globalProjectDir = createTempProjectDir("global");
        executorService = ConcurrencyUtil.newSameThreadExecutorService();
    }

    @Override
    protected void tearDown() throws Exception {
        executorService.shutdown();
        super.tearDown();
    }

    private String createTempProjectDir(String projectName) throws IOException {
        String tempProjectDir = getTempDir().createVirtualDir(projectName).toNioPath().toString();
        FileUtils.copyDirectory(GRADLE_ROOT.resolve(projectName).toFile(), new File(tempProjectDir));
        return tempProjectDir;
    }

    public void testGetGradleWrapperExeAndJdk() throws IOException {
        GradleProjectImportUtil.linkAndRefreshGradleProject(wrapperProjectDir, getProject());
        GradleScanner gradleScanner = new GradleScanner(getProject(), wrapperProjectDir, executorService);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanner.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertTrue(StringUtils.contains(gradleExe, "wrapper"));
        new GradleDriver(gradleExe, null).verifyGradleInstalled();
    }

    public void testGetGradleGlobalExeAndJdk() throws IOException {
        GradleProjectImportUtil.linkAndRefreshGradleProject(globalProjectDir, getProject());
        GradleScanner gradleScanner = new GradleScanner(getProject(), globalProjectDir, executorService);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanner.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertNotNull(gradleExe);
        new GradleDriver(gradleExe, null).verifyGradleInstalled();
    }

    public void testBuildTree() throws IOException {
        GradleProjectImportUtil.linkAndRefreshGradleProject(globalProjectDir, getProject());
        GradleScanner gradleScanner = new GradleScanner(getProject(), globalProjectDir, executorService);
        gradleScanner.setScanLogic(new GraphScanLogic(gradleScanner.getPackageType(), new NullLog()));

        // Run and check scan results
        DependencyTree results = gradleScanner.buildTree();
        assertNotNull(results);
        assertTrue(results.isMetadata());
        assertEquals(Paths.get(globalProjectDir).getFileName().toString(), results.getUserObject());
        assertScopes(results);
        assertEquals(3, results.getChildCount());

        // Check module dependency
        DependencyTree moduleNode = getAndAssertChild(results, "shared");
        assertTrue(results.isMetadata());
        assertEquals(1, moduleNode.getChildCount());

        // Check dependency
        DependencyTree dependencyNode = getAndAssertChild(moduleNode, "junit:junit:4.7");
        assertFalse(dependencyNode.isMetadata());
        assertContainsElements(dependencyNode.getScopes(), new Scope("TestImplementation"), new Scope("TestRuntimeClasspath"), new Scope("TestCompileClasspath"));
    }
}
