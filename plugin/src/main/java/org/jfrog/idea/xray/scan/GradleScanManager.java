package org.jfrog.idea.xray.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.impl.services.summary.ComponentDetailImpl;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jfrog.idea.xray.ScanTreeNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
public class GradleScanManager extends ScanManager {

    private Collection<DataNode<LibraryDependencyData>> libraryDependencies;
    private ScanTreeNode rootNode = new ScanTreeNode(ScanManager.ROOT_NODE_HEADER);

    public GradleScanManager(Project project) {
        super(project);
    }

    public static boolean isApplicable(@NotNull Project project) {
        GradleSettings.MyState state = GradleSettings.getInstance(project).getState();
        return state != null && !state.getLinkedExternalProjectsSettings().isEmpty();
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        if (libraryDependencies != null) {
            // Change the dependencies only if there are new dependencies
            this.libraryDependencies = libraryDependencies;
        }
        if (this.libraryDependencies == null) {
            ExternalSystemUtil.refreshProject(project, GradleConstants.SYSTEM_ID, getProjectBasePath(project), cbk, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);
        } else {
            cbk.onSuccess(null);
        }
    }

    @Override
    protected Components collectComponentsToScan(@Nullable DataNode<ProjectData> externalProject) {
        Components components = ComponentsFactory.create();
        rootNode = new ScanTreeNode(ScanManager.ROOT_NODE_HEADER);
        if (libraryDependencies == null) {
            libraryDependencies = ExternalSystemApiUtil.findAllRecursively(externalProject, ProjectKeys.LIBRARY_DEPENDENCY);
        }
        libraryDependencies.stream()
                .filter(GradleScanManager::isRootDependency)
                .filter(distinctByName(dataNode -> dataNode.getData().getExternalName()))
                .forEach(dataNode -> populateDependenciesTree(rootNode, dataNode));
        addAllArtifacts(components, rootNode, GAV_PREFIX);
        return components;
    }

    @Override
    protected TreeModel updateResultsTree(TreeModel currentScanResults) {
        scanTree(rootNode);
        return new DefaultTreeModel(rootNode, false);
    }

    private static boolean isRootDependency(DataNode<LibraryDependencyData> dataNode) {
        return dataNode.getParent() == null || !ProjectKeys.LIBRARY_DEPENDENCY.equals(dataNode.getParent().getKey());
    }

    private static <T> Predicate<T> distinctByName(Function<? super T, ?> getNameFunc) {
        Set<Object> seen = Sets.newHashSet();
        return t -> seen.add(getNameFunc.apply(t));
    }

    private void populateDependenciesTree(ScanTreeNode scanTreeNode, DataNode<? extends AbstractDependencyData> dataNode) {
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
                logger.warn("Bad component ID structure. Should be <GroupID>:<ArtifactID>:<Version>, got '" + componentId + "'");
            }
            return;
        }

        ComponentDetailImpl scanComponent = new ComponentDetailImpl(componentId, getArtifactChecksum(dataNode));
        ScanTreeNode treeNode = new ScanTreeNode(scanComponent);
        for (DataNode child : dataNode.getChildren()) {
            populateDependenciesTree(treeNode, child);
        }
        scanTreeNode.add(treeNode);
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