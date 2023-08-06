package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.gradle.GradleTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.GradleGroovyInspection;
import com.jfrog.ide.idea.inspections.GradleKotlinInspection;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;
import org.jetbrains.plugins.gradle.service.settings.GradleConfigurable;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static com.jfrog.ide.common.log.Utils.logError;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanner extends SingleDescriptorScanner {
    private final GradleTreeBuilder gradleTreeBuilder;
    private boolean kotlin;

    /**
     * @param project   currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                  like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param basePath  the build.gradle or build.gradle.kts directory
     * @param executor  an executor that should limit the number of running tasks to 3
     * @param scanLogic the scan logic to use
     */
    GradleScanner(Project project, String basePath, ExecutorService executor, ScanLogic scanLogic) {
        super(project, basePath, ComponentPrefix.GAV, executor, scanLogic);
        getLog().info("Found Gradle project: " + getProjectPath());
        Path dirPath = Paths.get(this.basePath);
        Path buildGradleKotlinPath = dirPath.resolve("build.gradle.kts");
        if (Files.exists(buildGradleKotlinPath)) {
            descriptorFilePath = buildGradleKotlinPath.toString();
        } else {
            descriptorFilePath = dirPath.resolve("build.gradle").toString();
        }
        Map<String, String> env = Maps.newHashMap(EnvironmentUtil.getEnvironmentMap());
        Path pluginLibDir = PluginManagerCore.getPlugin(PluginId.findId("org.jfrog.idea")).getPluginPath().resolve("lib");
        env.put("pluginLibDir", pluginLibDir.toAbsolutePath().toString());
        gradleTreeBuilder = new GradleTreeBuilder(Paths.get(basePath), descriptorFilePath, env, getGradleExeAndJdk(env));
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        Path basePath = Paths.get(this.basePath);
        VirtualFile file = localFileSystem.findFileByPath(basePath.resolve("build.gradle").toString());
        if (file == null) {
            file = localFileSystem.findFileByPath(basePath.resolve("build.gradle.kts").toString());
            if (file == null) {
                return null;
            }
            kotlin = true;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected AbstractInspection getInspectionTool() {
        return kotlin ? new GradleKotlinInspection() : new GradleGroovyInspection();
    }

    @Override
    protected PackageManagerType getPackageManagerType() {
        return PackageManagerType.GRADLE;
    }

    @Override
    protected DepTree buildTree() throws IOException {
        return gradleTreeBuilder.buildTree(getLog());
    }

    /**
     * Extract the chosen Gradle executable path from the Gradle plugin. If Gradle is not configured well, return null.
     *
     * @param env - The environment variables map to set the JAVA_HOME
     * @return the chosen Gradle executable path or null
     */
    String getGradleExeAndJdk(Map<String, String> env) {
        File gradleHome = resolveGradleAndSetJavaHome(env);
        if (gradleHome == null) {
            getLog().info("Using Gradle from system path.");
            return null;
        }
        String gradleExe = gradleHome.toPath().resolve("bin").resolve(SystemUtils.IS_OS_WINDOWS ? "gradle.bat" : "gradle").toString();
        getLog().info("Using Gradle executable " + gradleExe);
        return gradleExe;
    }

    /**
     * Resolve Gradle executable and Java home from Gradle settings.
     *
     * @param env - The environment variables to set the JAVA_HOME
     * @return gradle executable
     */
    private File resolveGradleAndSetJavaHome(Map<String, String> env) {
        GradleSettings gradleSettings = GradleSettings.getInstance(project);
        GradleProjectSettings projectSettings = gradleSettings.getLinkedProjectSettings(basePath);
        if (projectSettings == null && SystemUtils.IS_OS_WINDOWS) {
            projectSettings = gradleSettings.getLinkedProjectSettings(basePath.replaceAll("\\\\", "/"));
        }
        if (projectSettings == null) {
            logError(getLog(), "Couldn't retrieve Gradle project settings. Hint - make sure the Gradle project was properly imported.", false);
            return null;
        }
        GradleInstallationManager gradleInstallationManager = ApplicationManager.getApplication().getService(GradleInstallationManager.class);

        // Set JAVA_HOME
        String javaHome = gradleInstallationManager.getGradleJvmPath(project, projectSettings.getExternalProjectPath());
        if (StringUtils.isNotBlank(javaHome)) {
            getLog().info("Using Java home: " + javaHome);
            env.put("JAVA_HOME", javaHome);
        }

        File gradleHome = gradleInstallationManager.getGradleHome(project, projectSettings.getExternalProjectPath());
        if (gradleHome != null) {
            return gradleHome;
        }
        if (StringUtils.isNotBlank(projectSettings.getGradleHome())) {
            return new File(projectSettings.getGradleHome());
        }

        // Gradle wasn't set properly
        if (isMisconfigurationError(projectSettings.getExternalProjectPath())) {
            Logger.addOpenSettingsLink("It looks like Gradle home was not properly set in your project. " +
                    "Click <a href=\"#settings\">here</a> to set Gradle home.", project, GradleConfigurable.class);
        } else {
            getLog().warn("Can't run Gradle from Gradle settings. Hint - try to reload Gradle project and then refresh the scan.");
        }
        return null;
    }

    private boolean isMisconfigurationError(String linkedProjectPath) {
        GradleProjectSettings projectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(linkedProjectPath);
        if (projectSettings != null) {
            DistributionType distributionType = projectSettings.getDistributionType();
            // Distribution type has not been chosen or the distribution type is not Gradle wrapper.
            // If the distribution type is wrapped, it is probable that the Gradle wrapper is not yet created.
            return distributionType == null || !distributionType.isWrapped();
        }
        // Gradle project settings are not set
        return true;
    }
}
