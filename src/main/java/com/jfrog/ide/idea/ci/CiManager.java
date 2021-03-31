package com.jfrog.ide.idea.ci;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.ci.BuildDependencyTree;
import com.jfrog.ide.common.ci.BuildGeneralInfo;
import com.jfrog.ide.common.ci.CiManagerBase;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.configuration.GlobalSettings;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.events.BuildEvents;
import com.jfrog.ide.idea.events.ProjectEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.log.ProgressIndicatorImpl;
import com.jfrog.ide.idea.ui.CiComponentsTree;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.filters.filtermanager.CiFilterManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.idea.ui.configuration.JFrogProjectConfiguration.BUILDS_PATTERN_KEY;

/**
 * @author yahavi
 */
@State(name = "CiState")
public class CiManager extends CiManagerBase {
    private static final String LOAD_BUILD_FAIL_FMT = "Failed to load build '%s/%s'.";
    private static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");

    // Lock to prevent multiple simultaneous scans
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);

    private final PropertiesComponent propertiesComponent;
    private final Project mainProject;

    private CiManager(@NotNull Project project) throws IOException {
        super(HOME_PATH.resolve("ci-cache"), project.getName(), Logger.getInstance(), GlobalSettings.getInstance().getServerConfig());
        this.propertiesComponent = PropertiesComponent.getInstance(project);
        this.mainProject = project;
        registerOnChangeHandlers();
    }

    public static CiManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CiManager.class);
    }

    public void asyncRefreshBuilds() {
        if (!scanPreconditionsMet()) {
            return;
        }
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, "Downloading builds information...") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                try {
                    if (mainProject.isDisposed()) {
                        return;
                    }
                    String buildsPattern = propertiesComponent.getValue(BUILDS_PATTERN_KEY);
                    buildCiTree(buildsPattern, new ProgressIndicatorImpl(indicator));
                    CiFilterManager.getInstance(mainProject).collectBuildsInformation(root);
                    BuildDependencyTree dependencyTree = (BuildDependencyTree) root.getFirstChild();
                    loadBuild(dependencyTree.getGeneralInfo());
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    Logger.getInstance().error("Failed to refresh builds", e);
                } finally {
                    scanInProgress.set(false);
                }
            }
        };
        // The progress manager is only good for foreground threads.
        if (SwingUtilities.isEventDispatchThread()) {
            ProgressManager.getInstance().run(scanAndUpdateTask);
        } else {
            // Run the scan task when the thread is in the foreground.
            ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(scanAndUpdateTask));
        }
    }

    /**
     * Load a build from the cache to the UI tree after selecting it in the builds selector.
     * To save RAM, we save only 1 build dependency tree simultaneously.
     *
     * @param buildGeneralInfo - The build general info
     */
    public void loadBuild(GeneralInfo buildGeneralInfo) {
        ComponentsTree componentsTree = CiComponentsTree.getInstance(mainProject);
        componentsTree.reset();
        try {
            BuildDependencyTree buildTree = loadBuildTree(buildGeneralInfo.getArtifactId(), buildGeneralInfo.getVersion());
            if (buildTree.isLeaf()) {
                return;
            }
            CiFilterManager.getInstance(mainProject).collectsFiltersInformation(buildTree);
            componentsTree.addScanResults(mainProject.getName(), buildTree);
            MessageBus projectMessageBus = mainProject.getMessageBus();
            ProjectsMap.ProjectKey projectKey = ProjectsMap.createKey(mainProject.getName(), buildTree.getGeneralInfo());
            projectMessageBus.syncPublisher(ProjectEvents.ON_SCAN_CI_CHANGE).update(projectKey);
        } catch (IOException | ParseException | IllegalArgumentException e) {
            Logger.getInstance().error(String.format(LOAD_BUILD_FAIL_FMT, buildGeneralInfo.getArtifactId(), buildGeneralInfo.getVersion()), e);
        }
    }

    /**
     * Search the build general info in the root tree.
     *
     * @param buildIdentifier - <buildName>/<buildNumber>
     * @return the build general info or null
     */
    public BuildGeneralInfo getBuildGeneralInfo(String buildIdentifier) {
        String[] buildSplit = buildIdentifier.split("/");
        String buildName = buildSplit[0];
        String buildNumber = buildSplit[1];
        return (BuildGeneralInfo) root.getChildren().stream()
                .map(DependencyTree::getGeneralInfo)
                .filter(generalInfo -> StringUtils.equals(buildName, generalInfo.getArtifactId()))
                .filter(generalInfo -> StringUtils.equals(buildNumber, generalInfo.getVersion()))
                .findAny().orElse(null);
    }

    private boolean scanPreconditionsMet() {
        if (!GlobalSettings.getInstance().areArtifactoryCredentialsSet()) {
            Logger.getInstance().warn("Artifactory server is not configured.");
            return false;
        }
        if (StringUtils.isBlank(propertiesComponent.getValue(BUILDS_PATTERN_KEY))) {
            Logger.getInstance().warn("Builds pattern is not set.");
            return false;
        }
        if (!scanInProgress.compareAndSet(false, true)) {
            Logger.getInstance().info("Builds scan is already in progress.");
            return false;
        }
        return true;
    }

    private void registerOnChangeHandlers() {
        MessageBusConnection busConnection = ApplicationManager.getApplication().getMessageBus().connect();
        busConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, this::asyncRefreshBuilds);
        MessageBusConnection projectBusConnection = mainProject.getMessageBus().connect();
        projectBusConnection.subscribe(ApplicationEvents.ON_BUILDS_CONFIGURATION_CHANGE, this::asyncRefreshBuilds);
        projectBusConnection.subscribe(BuildEvents.ON_SELECTED_BUILD, this::loadBuild);
    }
}
