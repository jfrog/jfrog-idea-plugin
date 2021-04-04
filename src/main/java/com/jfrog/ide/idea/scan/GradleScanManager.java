package com.jfrog.ide.idea.scan;

import com.google.common.collect.*;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.dependencies.ArtifactDependencyNode;
import com.intellij.openapi.externalSystem.model.project.dependencies.ComponentDependencies;
import com.intellij.openapi.externalSystem.model.project.dependencies.DependencyNode;
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.GradleInspection;
import com.jfrog.ide.idea.utils.Utils;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jfrog.ide.idea.utils.Utils.getProjectBasePath;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private Collection<DataNode<ProjectDependencies>> dependenciesData;
    private Map<String, DependencyTree> modules = Maps.newHashMap();

    GradleScanManager(Project project) throws IOException {
        super(project, project, ComponentPrefix.GAV);
    }

    static boolean isApplicable(@NotNull Project project) {
        GradleSettings.MyState state = GradleSettings.getInstance(project).getState();
        return state != null && !state.getLinkedExternalProjectsSettings().isEmpty();
    }

    /**
     * Returns all project modules locations as Paths.
     * Other scanners such as npm will use this paths in order to find modules.
     *
     * @return all project modules locations as Paths
     */
    public Set<Path> getProjectPaths() {
        Set<Path> paths = super.getProjectPaths();
        GradleSettings.MyState gradleState = GradleSettings.getInstance(project).getState();
        if (gradleState != null) {
            gradleState.getLinkedExternalProjectsSettings()
                    .stream()
                    .map(ExternalProjectSettings::getModules)
                    .forEach(module -> paths.addAll(module.stream()
                            .map(Paths::get)
                            .collect(Collectors.toSet())));
        } else {
            getLog().warn("Gradle state is null");
        }
        return paths;
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<ProjectDependencies>> dependenciesData) {
        if (dependenciesData != null) {
            // Change the dependencies only if there are new dependencies
            this.dependenciesData = dependenciesData;
        }
        if (this.dependenciesData != null) {
            cbk.onSuccess(null);
            return;
        }
        ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
        if (processingManager != null && processingManager.findTask(ExternalSystemTaskType.RESOLVE_PROJECT, GradleConstants.SYSTEM_ID, getProjectBasePath(project).toString()) != null) {
            // Another scan in progress
            return;
        }
        ExternalSystemUtil.refreshProject(project, GradleConstants.SYSTEM_ID, getProjectBasePath(project).toString(), cbk, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        String buildGradlePath = Paths.get(Utils.getProjectBasePath(project).toString(), "build.gradle").toString();
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildGradlePath);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return new PsiFile[]{psiFile};
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new GradleInspection();
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) {
        collectDependenciesIfMissing(externalProject);
        dependenciesData.forEach(this::populateModulesWithDependencies);
        DependencyTree rootNode = new DependencyTree(project.getName());
        modules.values().forEach(rootNode::add);
        GeneralInfo generalInfo = new GeneralInfo().name(project.getName()).path(Utils.getProjectBasePath(project).toString());
        rootNode.setGeneralInfo(generalInfo);
        if (rootNode.getChildren().size() == 1) {
            setScanResults((DependencyTree) rootNode.getChildAt(0));
        } else {
            setScanResults(rootNode);
        }
    }

    private void populateModulesWithDependencies(DataNode<ProjectDependencies> dataNode) {
        // The DependencyNode's equals and hash functions compares the node's children, an integer number and some other
        // non-relevant fields. Comparing with these functions causes unwanted duplications in the dependency tree.
        // Therefore we will use TreeMultimap with custom comparators.
        Multimap<DependencyNode, Scope> moduleDependencies = TreeMultimap.create(
                (DependencyNode key1, DependencyNode key2) -> StringUtils.compare(key1.getDisplayName(), key2.getDisplayName()),
                (Scope value1, Scope value2) -> StringUtils.compare(value1.getName(), value2.getName()));
        ProjectDependencies projectDependencies = dataNode.getData();
        String moduleId = getModuleId(dataNode);
        if (!modules.containsKey(moduleId)) {
            return;
        }

        // Collect dependencies from project components ('main' and 'test').
        for (ComponentDependencies componentDependency : projectDependencies.getComponentsDependencies()) {
            Stream.concat(componentDependency.getCompileDependenciesGraph().getDependencies().stream(), componentDependency.getRuntimeDependenciesGraph().getDependencies().stream())
                    .filter(GradleScanManager::isArtifactDependencyNode)
                    .forEach(dependencyNode -> moduleDependencies.put(dependencyNode, new Scope(componentDependency.getComponentName())));
        }
        // Populate dependencies-tree for all modules.
        moduleDependencies.asMap().forEach((key, value) -> populateDependencyTree(modules.get(moduleId), key, (Set<Scope>) value));
    }

    private void populateDependencyTree(DependencyTree DependencyTree, DependencyNode dependencyNode, Set<Scope> scopes) {
        ComponentDetailImpl scanComponent = new ComponentDetailImpl(dependencyNode.getDisplayName(), "");
        DependencyTree treeNode = new DependencyTree(scanComponent);
        if (scopes != null) {
            treeNode.setScopes(scopes);
        }

        // Recursively search for dependencies and add to tree.
        List<DependencyNode> childrenList = dependencyNode.getDependencies().stream()
                .filter(GradleScanManager::isArtifactDependencyNode)
                .collect(Collectors.toList());
        childrenList.forEach(child -> populateDependencyTree(treeNode, child, null));

        DependencyTree.add(treeNode);
    }

    private static boolean isArtifactDependencyNode(DependencyNode dependencyNode) {
        return dependencyNode instanceof ArtifactDependencyNode;
    }

    private void collectDependenciesIfMissing(DataNode<ProjectData> externalProject) {
        if (dependenciesData == null) {
            collectModuleDependencies(externalProject);
            collectDependenciesData(externalProject);
        } else {
            modules.values().forEach(child -> child.getChildren().clear());
        }
    }

    /**
     * Collect Gradle modules dependencies. Used in user click on the refresh button.
     *
     * @param externalProject - The Gradle root node
     */
    private void collectModuleDependencies(DataNode<ProjectData> externalProject) {
        Collection<DataNode<ModuleData>> moduleDependencies = ExternalSystemApiUtil.findAllRecursively(externalProject, ProjectKeys.MODULE);
        modules = Maps.newHashMap();
        moduleDependencies.forEach(module -> {
            String groupId = Objects.toString(module.getData().getGroup(), "");
            String artifactId = StringUtils.removeStart(module.getData().getId(), ":");
            String version = Objects.toString(module.getData().getVersion(), "");
            DependencyTree scanTreeNode = new DependencyTree(artifactId);
            scanTreeNode.setGeneralInfo(new GeneralInfo().pkgType("gradle").groupId(groupId).artifactId(artifactId).version(version));
            modules.put(StringUtils.removeStart(module.getData().getId(), ":"), scanTreeNode);
        });
    }

    private void collectDependenciesData(DataNode<ProjectData> externalProject) {
        this.dependenciesData = ExternalSystemApiUtil.findAllRecursively(externalProject, ProjectKeys.DEPENDENCIES_GRAPH);
    }

    private static String getModuleId(DataNode<ProjectDependencies> dataNode) {
        DataNode<ModuleData> moduleDataNode = dataNode.getDataNode(ProjectKeys.MODULE);
        if (moduleDataNode == null) {
            return "";
        }
        return StringUtils.removeStart(moduleDataNode.getData().getId(), ":");
    }
}