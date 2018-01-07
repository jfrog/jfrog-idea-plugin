package org.jfrog.idea.xray.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.utils.FileChangeListener;
import org.jfrog.idea.xray.utils.Utils;
import org.jfrog.idea.xray.utils.npm.NpmDriver;
import org.jfrog.idea.xray.utils.npm.NpmPackageFileFinder;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    public static final String INSTALLATION_DIR = Paths.get(".idea", "jfrog", "npm").toString(); // The directory that the Npm applications will be installed.
    private static final String[] NPM_FILES = {"package.json", "package-lock.json", ".npmrc"}; // Files to copy to installation dir. We execute 'install' and 'ls' on these files.
    static final String NPM_PREFIX = "npm://";
    ScanTreeNode rootNode = new ScanTreeNode(ROOT_NODE_HEADER);
    private NpmDriver npmDriver;

    public NpmScanManager(Project project) {
        super(project);
        npmDriver = new NpmDriver();
        try {
            Path installationPath = Paths.get(getProjectBasePath(project), INSTALLATION_DIR);
            Files.createDirectories(installationPath);
        } catch (IOException e) {
            Utils.notify(logger, "Failed to create installation directory", e, NotificationType.ERROR);
        }
        FileChangeListener.Callback asyncScanCbk = getFileListenerCbk();
        VirtualFileManager.getInstance().addVirtualFileListener(new FileChangeListener(Arrays.asList(NPM_FILES), asyncScanCbk));
    }

    private NpmScanManager() {
        super();
    }

    public static NpmScanManager CreateNpmScanManager(Project project) throws IOException {
        NpmScanManager npmScanManager = new NpmScanManager();
        npmScanManager.project = project;
        npmScanManager.npmDriver = new NpmDriver();
        Path InstallationPath = Paths.get(getProjectBasePath(project), INSTALLATION_DIR);
        Files.createDirectories(InstallationPath);
        return npmScanManager;
    }

    public static boolean isApplicable(@NotNull Project project) {
        // 1. Check that npm is installed.
        if (!NpmDriver.isNpmInstalled()) {
            return false;
        }
        // 2. Check for existence of package.json files.
        List<String> packageAppsPairs = Lists.newArrayList(); // List of package.json's directories.
        try {
            String basePath = getProjectBasePath(project);
            findApplicationDirs(Paths.get(basePath), packageAppsPairs);
        } catch (IOException e) {
            return false;
        }
        return !packageAppsPairs.isEmpty();
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        try {
            rootNode = new ScanTreeNode(ROOT_NODE_HEADER);
            String projectBasePath = getProjectBasePath(project);
            List<String> applicationsDirs = Lists.newArrayList(); // Lists of package.json parent directories
            findApplicationDirs(Paths.get(projectBasePath), applicationsDirs);
            // This set is used to make sure the artifacts added are unique
            Set<String> added = Sets.newHashSet();
            for (String appDir : applicationsDirs) {
                checkCanceled();
                Path relativeSource = Paths.get(projectBasePath).relativize(Paths.get(appDir));
                Path dest = Paths.get(projectBasePath, INSTALLATION_DIR).resolve(relativeSource);
                copyNpmFiles(Paths.get(appDir), dest);
                npmDriver.install(dest.toString());
                JsonNode jsonRoot = npmDriver.list(dest.toString());
                JsonNode dependencies = jsonRoot.get("dependencies");
                if (dependencies != null) {
                    dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
                        String componentId = getComponentId(stringJsonNodeEntry);
                        if (added.add(componentId)) {
                            addSubtree(stringJsonNodeEntry, rootNode, componentId); // Populate the tree recursively
                        }
                    });
                }
            }
            cbk.onSuccess(null);
        } catch (ProcessCanceledException e) {
            Utils.notify(logger, "JFrog Xray","Xray scan was canceled", NotificationType.INFORMATION);
        } catch (Exception e) {
            cbk.onFailure(e.getMessage(), e.getCause().getMessage());
        }
    }

    @Override
    protected Components collectComponentsToScan(@Nullable DataNode<ProjectData> externalProject) {
        Components components = ComponentsFactory.create();
        addAllArtifacts(components, rootNode, NPM_PREFIX);
        return components;
    }

    @Override
    protected TreeModel updateResultsTree(TreeModel currentScanResults) {
        scanTree(rootNode);
        return new DefaultTreeModel(rootNode, false);
    }

    private void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, ScanTreeNode node, String componentId) {
        if (StringUtils.isBlank(componentId)) {
            return;
        }
        ComponentDetailImpl scanComponent = new ComponentDetailImpl(componentId, "");
        ScanTreeNode childTreeNode = new ScanTreeNode(scanComponent);
        JsonNode childDependencies = stringJsonNodeEntry.getValue().get("dependencies");
        populateDependenciesTree(childTreeNode, childDependencies); // Mutual recursive call
        node.add(childTreeNode);
    }

    private void populateDependenciesTree(ScanTreeNode scanTreeNode, @Nullable JsonNode dependencies) {
        if (dependencies == null) {
            return;
        }
        dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
            String componentId = getComponentId(stringJsonNodeEntry);
            addSubtree(stringJsonNodeEntry, scanTreeNode, componentId); // Mutual recursive call
        });
    }

    private String getComponentId(Map.Entry<String, JsonNode> stringJsonNodeEntry) {
        String artifactId = stringJsonNodeEntry.getKey();
        JsonNode version = stringJsonNodeEntry.getValue().get("version");
        return version == null ? "" : artifactId + ":" + version.textValue();
    }

    /**
     * Get package.json parent directories.
     * @param path - Input - The project base path
     * @param applicationsDirs - Output - List of package.json parent directories
     */
    private static void findApplicationDirs(Path path, List<String> applicationsDirs) throws IOException {
        NpmPackageFileFinder npmPackageFileFinder = new NpmPackageFileFinder(path);
        applicationsDirs.addAll(npmPackageFileFinder.getPackageFilePairs());
    }

    /**
     * Return a callback that handle fs operations on files.
     * Specifically in our case, each change in 'package.json', 'package-lock.json' or '.npmrc' trigger a call to this callback.
     * @return callback that handle fs operations on files
     */
    private FileChangeListener.Callback getFileListenerCbk() {
        return (virtualFileEvent, fileEventType) -> {
            if (project.isDisposed()) {
                return;
            }
            String projectBasePath = getProjectBasePath(project);
            Path originPath = Paths.get(virtualFileEvent.getFile().getPath());
            String relativeOriginPath = new File(projectBasePath).toURI().relativize(originPath.toUri()).getPath();
            Path installationFilePath = Paths.get(projectBasePath, INSTALLATION_DIR, relativeOriginPath);
            try {
                switch (fileEventType) {
                    case Copy:
                    case Create:
                    case Change:
                    case Move:
                        // Nothing to do. The 'Refresh Dependencies' copies all relevant files.
                        break;
                    case Delete:
                        Files.deleteIfExists(installationFilePath);
                        break;
                }

            } catch (IOException e) {
                Utils.notify(logger, "Failed to delete file", e, NotificationType.WARNING);
            }

            NpmScanManager.super.asyncScanAndUpdateResults(true);
        };
    }

    /**
     * Copy 'package.json', 'package-lock.json' or '.npmrc' files from application base dir to .idea/jfrog/npm/.
     * @param source the application base dir
     * @param dest path to the project '.idea/jfrog/npm/'
     */
    private static void copyNpmFiles(Path source, Path dest) throws IOException {
        Files.createDirectories(dest);
        for (String npmFile : NPM_FILES) {
            Path sourceNpmFile = source.resolve(npmFile);
            if (Files.exists(sourceNpmFile)) {
                Files.copy(sourceNpmFile, dest.resolve(npmFile), REPLACE_EXISTING, COPY_ATTRIBUTES);
            }
        }
    }
}