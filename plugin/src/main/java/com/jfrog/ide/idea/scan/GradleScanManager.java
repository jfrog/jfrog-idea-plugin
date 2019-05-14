package com.jfrog.ide.idea.scan;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private Map<String, DependenciesTree> modules;
    private Collection<DataNode<LibraryDependencyData>> libraryDependencies;
    private DependenciesTree rootNode = new DependenciesTree(ScanManager.ROOT_NODE_HEADER);

    public GradleScanManager(Project project) throws IOException {
        super(project, ComponentPrefix.GAV);
    }

    public static boolean isApplicable(@NotNull Project project) {
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
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        if (libraryDependencies != null) {
            // Change the dependencies only if there are new dependencies
            this.libraryDependencies = libraryDependencies;
            cbk.onSuccess(null);
            return;
        }
        if (this.libraryDependencies != null) {
            cbk.onSuccess(null);
            return;
        }
        ExternalSystemProcessingManager processingManager = ServiceManager.getService(ExternalSystemProcessingManager.class);
        if (processingManager != null && processingManager.findTask(ExternalSystemTaskType.RESOLVE_PROJECT, GradleConstants.SYSTEM_ID, getProjectBasePath(project)) != null) {
            // Another scan in progress
            return;
        }
        ExternalSystemUtil.refreshProject(project, GradleConstants.SYSTEM_ID, getProjectBasePath(project), cbk, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) {
//        Components components = ComponentsFactory.create();
        rootNode = new DependenciesTree(ScanManager.ROOT_NODE_HEADER);
        collectDependenciesIfMissing(externalProject);
        libraryDependencies.parallelStream()
                .filter(GradleScanManager::isLibraryDependency)
                .filter(GradleScanManager::isRootDependency)
                .map(dataNode -> Pair.of(getModuleId(dataNode), dataNode))
                .filter(distinctByName(pair -> pair.getLeft() + getDependencyName(pair.getRight())))
                .filter(pair -> modules.containsKey(pair.getLeft()))
                .forEach(pair -> populateDependenciesTree(modules.get(pair.getLeft()), pair.getRight()));
        modules.values().forEach(module -> rootNode.add(module));
//        addAllArtifacts(components, rootNode, GAV_PREFIX);
//        return components;
    }

    private void collectDependenciesIfMissing(DataNode<ProjectData> externalProject) {
        if (libraryDependencies == null) {
            collectModuleDependencies(externalProject);
            collectLibraryDependencies(externalProject);
        } else {
            modules.values().forEach(child -> child.getChildren().clear());
        }
    }

    private void collectModuleDependencies(DataNode<ProjectData> externalProject) {
        Collection<DataNode<ModuleData>> moduleDependencies = ExternalSystemApiUtil.findAllRecursively(externalProject, ProjectKeys.MODULE);
        modules = Maps.newHashMap();
        moduleDependencies.forEach(module -> {
            String groupId = Objects.toString(module.getData().getGroup(), "");
            String artifactId = StringUtils.removeStart(module.getData().getId(), ":");
            String version = Objects.toString(module.getData().getVersion(), "");
            String gav = groupId + ":" + artifactId + ":" + version;
            DependenciesTree scanTreeNode = new DependenciesTree(artifactId);
            scanTreeNode.setGeneralInfo(new GeneralInfo()
                    .componentId(gav)
                    .pkgType("gradle")
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version));
            modules.put(module.getData().getId(), scanTreeNode);
        });
    }

    private void collectLibraryDependencies(DataNode<ProjectData> externalProject) {
        libraryDependencies = ExternalSystemApiUtil.findAllRecursively(externalProject, ProjectKeys.LIBRARY_DEPENDENCY);
    }

    private static boolean isLibraryDependency(DataNode<LibraryDependencyData> dataNode) {
        return ProjectKeys.LIBRARY_DEPENDENCY.equals(dataNode.getKey());
    }

    private static boolean isRootDependency(DataNode<LibraryDependencyData> dataNode) {
        return dataNode.getParent() == null || !ProjectKeys.LIBRARY_DEPENDENCY.equals(dataNode.getParent().getKey());
    }

    private static <T> Predicate<T> distinctByName(Function<? super T, ?> getNameFunc) {
        Set<Object> seen = Sets.newHashSet();
        return t -> seen.add(getNameFunc.apply(t));
    }

    private static String getModuleId(DataNode<LibraryDependencyData> dataNode) {
        return dataNode.getDataNode(ProjectKeys.MODULE).getData().getId();
    }

    private String getDependencyName(DataNode<LibraryDependencyData> dataNode) {
        return dataNode.getData().getExternalName();
    }

    private void populateDependenciesTree(DependenciesTree dependenciesTree, DataNode<? extends AbstractDependencyData> dataNode) {
        String componentId = dataNode.getData().getExternalName();

        int colonCount = StringUtils.countMatches(componentId, ":");
        if (colonCount == 3) {
            // <Group ID>:<Artifact ID>:<Classifier>:<Version>. The classifier should be ignored.
            int secondColonIdx = componentId.indexOf(":", componentId.indexOf(":") + 1);
            int thirdColonIdx = componentId.indexOf(":", secondColonIdx + 1);
            componentId = componentId.substring(0, secondColonIdx) + componentId.substring(thirdColonIdx);
            colonCount--;
        }
        if (colonCount != 2) {
            if (StringUtils.isNotBlank(componentId)) {
                getLog().warn("Bad component ID structure: Should be <GroupID>:<ArtifactID>:<Version>, got '" + componentId + "'");
            }
            return;
        }

        ComponentDetailImpl scanComponent = new ComponentDetailImpl(componentId, getArtifactChecksum(dataNode));
        DependenciesTree treeNode = new DependenciesTree(scanComponent);
        for (DataNode child : dataNode.getChildren()) {
            populateDependenciesTree(treeNode, child);
        }
        dependenciesTree.add(treeNode);
    }

    // Currently, this method always return an empty string, since the checksum is not sent to Xray.
    private String getArtifactChecksum(DataNode<? extends AbstractDependencyData> node) {
        /*
        try {
            Set<String> dependencyPaths = node.getData().getTarget().getPaths(LibraryPathType.BINARY);
            File file = new File((String)dependencyPaths.toArray()[0]);
            return calculateSha1(file);
        } catch (Exception e) {
            // Do nothing
        }
        */
        return "";
    }
}