package org.jfrog.idea.xray.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.summary.Components;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.idea.xray.ScanTreeNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Collection;
import java.util.Set;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    public MavenScanManager(Project project) {
        super(project);
        MavenProjectsManager.getInstance(project).addManagerListener(new MavenProjectsListener());
    }

    public static boolean isApplicable(@NotNull Project project) {
        return MavenProjectsManager.getInstance(project).hasProjects();
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        cbk.onSuccess(null);
    }

    @Override
    protected Components collectComponentsToScan(@Nullable DataNode<ProjectData> externalProject) {
        Components components = ComponentsFactory.create();
        // This set is used to make sure the artifacts added are unique
        Set<String> added = Sets.newHashSet();
        for (MavenProject mavenProject : MavenProjectsManager.getInstance(project).getProjects()) {
            for (MavenArtifactNode mavenArtifactNode : mavenProject.getDependencyTree()) {
                if (added.add(mavenArtifactNode.getArtifact().getDisplayStringForLibraryName())) {
                    addArtifact(components, mavenArtifactNode.getArtifact());
                    for (MavenArtifactNode artifactNode : mavenArtifactNode.getDependencies()) {
                        addArtifact(components, artifactNode.getArtifact());
                    }
                }
            }
        }
        return components;
    }

    private void addArtifact(Components components, MavenArtifact artifact) {
        components.addComponent(GAV_PREFIX + artifact.getDisplayStringForLibraryName(), getArtifactChecksum(artifact));
    }

    @Override
    protected TreeModel updateResultsTree(TreeModel currentScanResults) {
        ScanTreeNode rootNode = new ScanTreeNode(ROOT_NODE_HEADER);
        TreeModel issuesTree = new DefaultTreeModel(rootNode);
        // This set is used to make sure the artifacts added are unique
        Set<String> added = Sets.newHashSet();
        for (MavenProject mavenProject : MavenProjectsManager.getInstance(project).getProjects()) {
            for (MavenArtifactNode dependencyTree : mavenProject.getDependencyTree()) {
                if (added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName())) {
                    updateChildrenNodes(rootNode, dependencyTree);
                }
            }
        }
        return issuesTree;
    }

    private void updateChildrenNodes(ScanTreeNode parentNode, MavenArtifactNode mavenArtifactNode) {
        ScanTreeNode currentNode = new ScanTreeNode(mavenArtifactNode.getArtifact().getDisplayStringForLibraryName());
        populateScanTreeNode(currentNode);
        for (MavenArtifactNode childrenArtifactNode : mavenArtifactNode.getDependencies()) {
            updateChildrenNodes(currentNode, childrenArtifactNode);
        }
        parentNode.add(currentNode);
    }

    // Currently, this method always return an empty string, since the checksum is not sent to Xray.
    private String getArtifactChecksum(MavenArtifact artifact) {
        /*
        try {
            return calculateSha1(artifact.getFile());
        } catch (Exception e) {
            // Do nothing
        }
        */
        return "";
    }

    /**
     * Maven project listener for scanning artifacts on dependencies changes.
     */
    private class MavenProjectsListener implements MavenProjectsManager.Listener {

        @Override
        public void activated() {
        }

        @Override
        public void projectsScheduled() {
        }

        @Override
        public void importAndResolveScheduled() {
            asyncScanAndUpdateResults(true);
        }
    }
}