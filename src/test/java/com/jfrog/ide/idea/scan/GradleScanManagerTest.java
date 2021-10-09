package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.io.FileUtils;
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

import static com.jfrog.ide.idea.TestUtils.assertScopes;
import static com.jfrog.ide.idea.TestUtils.getAndAssertChild;

/**
 * @author yahavi
 **/
public class GradleScanManagerTest extends HeavyPlatformTestCase {
    private static final Path GRADLE_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "gradle"));
    private String wrapperProjectDir;
    private String globalProjectDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wrapperProjectDir = createTempProjectDir("wrapper");
        globalProjectDir = createTempProjectDir("global");
    }

    private String createTempProjectDir(String projectName) throws IOException {
        String tempProjectDir = getTempDir().createVirtualDir(projectName).toNioPath().toString();
        FileUtils.copyDirectory(GRADLE_ROOT.resolve(projectName).toFile(), new File(tempProjectDir));
        return tempProjectDir;
    }

    public void testGetGradleWrapperExeAndJdk() {
        GradleProjectImportUtil.linkAndRefreshGradleProject(wrapperProjectDir, getProject());
        GradleScanManager gradleScanManager = new GradleScanManager(getProject(), wrapperProjectDir, null);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanManager.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertNotNull(gradleExe);
    }

    public void testGetGradleGlobalExeAndJdk() {
        GradleProjectImportUtil.linkAndRefreshGradleProject(globalProjectDir, getProject());
        GradleScanManager gradleScanManager = new GradleScanManager(getProject(), globalProjectDir, null);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanManager.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertNotNull(gradleExe);
    }

    public void testBuildTree() throws IOException {
        GradleProjectImportUtil.linkAndRefreshGradleProject(globalProjectDir, getProject());
        GradleScanManager gradleScanManager = new GradleScanManager(getProject(), globalProjectDir, new GraphScanLogic(null, new NullLog()));
        gradleScanManager.buildTree(false);

        // Run and check scan results
        DependencyTree results = gradleScanManager.getScanResults();
        assertNotNull(results);
        assertTrue(results.isMetadata());
        assertEquals(Paths.get(globalProjectDir).getFileName().toString(), results.getUserObject());
        assertScopes(results);
        assertEquals(5, results.getChildCount());

        // Check module dependency
        DependencyTree moduleNode = getAndAssertChild(results, "shared");
        assertTrue(results.isMetadata());
        assertEquals(1, moduleNode.getChildCount());

        // Check dependency
        DependencyTree dependencyNode = getAndAssertChild(moduleNode, "junit:junit:4.7");
        assertFalse(dependencyNode.isMetadata());
        assertContainsElements(dependencyNode.getScopes(), new Scope("Compile"), new Scope("Runtime"));
    }
}
