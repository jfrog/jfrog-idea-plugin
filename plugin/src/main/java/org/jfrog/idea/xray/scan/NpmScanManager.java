package org.jfrog.idea.xray.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.utils.Utils;
import org.jfrog.idea.xray.utils.npm.NpmDriver;
import org.jfrog.idea.xray.utils.npm.NpmPackageFileFinder;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 13 Dec 2017.
 */
public class NpmScanManager extends ScanManager {

    static final String NPM_PREFIX = "npm://";
    ScanTreeNode rootNode = new ScanTreeNode(ROOT_NODE_HEADER);
    private NpmDriver npmDriver;

    public NpmScanManager(Project project) {
        super(project);
        npmDriver = new NpmDriver();
    }

    private NpmScanManager() {
        super();
    }

    static NpmScanManager CreateNpmScanManager(Project project) {
        NpmScanManager npmScanManager = new NpmScanManager();
        npmScanManager.project = project;
        npmScanManager.npmDriver = new NpmDriver();
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
            for (String appDir : applicationsDirs) {
                checkCanceled();
                JsonNode jsonRoot = npmDriver.list(appDir);
                String packageName = jsonRoot.get("name").asText();
                if (jsonRoot.get("problems") != null) {
                    packageName += " (Installation required)";
                }
                ScanTreeNode module = new ScanTreeNode(packageName);
                module.setModuleName(packageName);
                rootNode.add(module);
                JsonNode dependencies = jsonRoot.get("dependencies");
                if (dependencies != null) {
                    dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
                        String componentId = getComponentId(stringJsonNodeEntry);
                        addSubtree(stringJsonNodeEntry, module, componentId); // Populate the tree recursively
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
}