package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ConcurrencyUtil;
import com.jfrog.ide.common.gradle.GradleDriver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author yahavi
 **/
public class GradleScanManagerTest extends HeavyPlatformTestCase {
    private static final Path GRADLE_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "gradle"));
    private ExecutorService executorService;
    private String wrapperProjectDir;
    private String globalProjectDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wrapperProjectDir = createTempProjectDir("wrapper");
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
        GradleScanManager gradleScanManager = new GradleScanManager(getProject(), wrapperProjectDir, executorService);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanManager.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertTrue(StringUtils.contains(gradleExe, "wrapper"));
        new GradleDriver(gradleExe, null).verifyGradleInstalled();
    }

    public void testGetGradleGlobalExeAndJdk() throws IOException {
        GradleProjectImportUtil.linkAndRefreshGradleProject(globalProjectDir, getProject());
        GradleScanManager gradleScanManager = new GradleScanManager(getProject(), globalProjectDir, executorService);
        Map<String, String> env = new HashMap<>();
        String gradleExe = gradleScanManager.getGradleExeAndJdk(env);
        assertEquals(System.getenv("JAVA_HOME"), env.get("JAVA_HOME"));
        assertNotNull(gradleExe);
        new GradleDriver(gradleExe, null).verifyGradleInstalled();
    }
}
