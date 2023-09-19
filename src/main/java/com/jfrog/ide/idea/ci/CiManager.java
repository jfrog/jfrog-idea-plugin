package com.jfrog.ide.idea.ci;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.progress.ProcessCanceledException;
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
import com.jfrog.ide.idea.ui.menus.filtermanager.CiFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jfrog.ide.common.log.Utils.logError;
import static com.jfrog.ide.idea.ui.configuration.JFrogProjectConfiguration.BUILDS_PATTERN_KEY;
import static com.jfrog.ide.idea.utils.Utils.HOME_PATH;

/**
 * @author yahavi
 */
@State(name = "CiState")
public class CiManager extends CiManagerBase implements Disposable {
    private static final String LOAD_BUILD_FAIL_FMT = "Failed to load build '%s/%s'.";

    // Lock to prevent multiple simultaneous scans
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final MessageBusConnection projectBusConnection;
    private final PropertiesComponent propertiesComponent;
    private final MessageBusConnection appBusConnection;
    private final Project project;

    private CiManager(@NotNull Project project) throws IOException {
        super(HOME_PATH.resolve("ci-cache"), project.getName(), Logger.getInstance(), GlobalSettings.getInstance().getServerConfig());
        this.propertiesComponent = PropertiesComponent.getInstance(project);
        this.projectBusConnection = project.getMessageBus().connect(this);
        this.appBusConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        this.project = project;
        registerOnChangeHandlers();
    }

    public static CiManager getInstance(@NotNull Project project) {
        return project.getService(CiManager.class);
    }

    public void asyncRefreshBuilds() {
        if (!scanPreconditionsMet()) {
            return;
        }
        Task.Backgroundable scanAndUpdateTask = new Task.Backgroundable(null, "Downloading builds information...") {
            @Override
            public void run(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                try {
                    if (project.isDisposed()) {
                        return;
                    }
                    project.getMessageBus().syncPublisher(ApplicationEvents.ON_SCAN_CI_STARTED).update();
                    String buildsPattern = propertiesComponent.getValue(BUILDS_PATTERN_KEY);
                    buildCiTree(buildsPattern, GlobalSettings.getInstance().getServerConfig().getProject(),
                            new ProgressIndicatorImpl(indicator), () -> checkCanceled(indicator));
                    CiFilterManager.getInstance(project).collectBuildsInformation(root);
                    loadFirstBuild();
                    sendUsageReport();
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    logError(Logger.getInstance(), "Failed to refresh builds", e, true);
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
     * Check if "cancel" was clicked.
     *
     * @param indicator - The progress indicator
     * @throws CancellationException in case the scan process should be canceled.
     */
    private void checkCanceled(com.intellij.openapi.progress.ProgressIndicator indicator) throws CancellationException {
        try {
            indicator.checkCanceled();
        } catch (ProcessCanceledException ignored) {
            throw new CancellationException();
        }
    }

    /**
     * Load a build from the cache to the UI tree after selecting it in the builds selector.
     * To save RAM, we save only 1 build dependency tree simultaneously.
     *
     * @param buildGeneralInfo - The build general info
     */
    public void loadBuild(BuildGeneralInfo buildGeneralInfo) {
        CiComponentsTree componentsTree = CiComponentsTree.getInstance(project);
        componentsTree.reset();
        ProjectsMap.ProjectKey projectKey = null;
        if (buildGeneralInfo != null) {
            try {
                BuildDependencyTree buildTree = loadBuildTree(buildGeneralInfo);
                CiFilterManager.getInstance(project).collectsFiltersInformation(buildTree);
                componentsTree.addScanResults(project.getName(), buildTree);
                projectKey = ProjectsMap.createKey(project.getName(), buildTree.getGeneralInfo());
            } catch (IOException | ParseException | IllegalArgumentException e) {
                Logger.getInstance().error(String.format(LOAD_BUILD_FAIL_FMT, buildGeneralInfo.getBuildName(), buildGeneralInfo.getBuildNumber()), e);
            }
        }
        MessageBus projectMessageBus = project.getMessageBus();
        projectMessageBus.syncPublisher(ProjectEvents.ON_SCAN_CI_CHANGE).update(projectKey);
    }

    /**
     * Search the build general info in the root tree.
     *
     * @param buildIdentifier - <buildName>/<buildNumber>
     * @return the build general info or null
     */
    public BuildGeneralInfo getBuildGeneralInfo(String buildIdentifier) {
        String buildName = StringUtils.substringBeforeLast(buildIdentifier, "/");
        String buildNumber = StringUtils.substringAfterLast(buildIdentifier, "/");
        return (BuildGeneralInfo) root.getChildren().stream()
                .map(DependencyTree::getGeneralInfo)
                .map(generalInfo -> (BuildGeneralInfo) generalInfo)
                .filter(generalInfo -> StringUtils.equals(buildName, generalInfo.getBuildName()))
                .filter(generalInfo -> StringUtils.equals(buildNumber, generalInfo.getBuildNumber()))
                .findAny().orElse(null);
    }

    /**
     * Load first build. If no builds found, delete all currently displayed build information from the UI.
     */
    private void loadFirstBuild() {
        BuildGeneralInfo generalInfo = null;
        if (!root.isLeaf()) {
            BuildDependencyTree dependencyTree = (BuildDependencyTree) root.getFirstChild();
            generalInfo = (BuildGeneralInfo) dependencyTree.getGeneralInfo();
        }
        project.getMessageBus().syncPublisher(BuildEvents.ON_SELECTED_BUILD).update(generalInfo);
    }

    private boolean scanPreconditionsMet() {
        if (!GlobalSettings.getInstance().areArtifactoryCredentialsSet()) {
            Logger.getInstance().debug("CI integration disabled - Artifactory server is not configured.");
            return false;
        }
        if (StringUtils.isBlank(propertiesComponent.getValue(BUILDS_PATTERN_KEY))) {
            Logger.getInstance().debug("CI integration disabled - build name pattern is not set. " +
                    "Configure it under the JFrog CI Integration page in the configuration.");
            return false;
        }
        if (!scanInProgress.compareAndSet(false, true)) {
            Logger.getInstance().info("Builds scan is already in progress.");
            return false;
        }
        return true;
    }

    private void registerOnChangeHandlers() {
        appBusConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, (ApplicationEvents) this::asyncRefreshBuilds);
        projectBusConnection.subscribe(ApplicationEvents.ON_BUILDS_CONFIGURATION_CHANGE, (ApplicationEvents) this::asyncRefreshBuilds);
        projectBusConnection.subscribe(BuildEvents.ON_SELECTED_BUILD, (BuildEvents) this::loadBuild);
    }

    @Override
    public void dispose() {
        // Disconnect and release resources from the project bus connection
        projectBusConnection.disconnect();
        // Disconnect and release resources from the application bus connection
        appBusConnection.disconnect();
    }

    private void sendUsageReport() {
        Utils.sendUsageReport("ci");
    }
}
