package org.jfrog.idea.xray.scan;

import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.summary.Components;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.persistency.types.Artifact;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import static org.jfrog.idea.xray.utils.Utils.calculateSha1;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    final private String GAV_PREFIX = "gav://";
    final private String ROOT_NODE_HEADER = "All components";

    public MavenScanManager(Project project) {
        super(project);
        MavenProjectsManager.getInstance(project).addManagerListener(new MavenProjectsListene());
    }

    @Override
    protected Components collectComponentsToScan() {
        Components components = ComponentsFactory.create();
        for (MavenProject mavenProject : MavenProjectsManager.getInstance(project).getProjects()) {
            for (MavenArtifactNode mavenArtifactNode : mavenProject.getDependencyTree()) {
                addArtifact(components, mavenArtifactNode.getArtifact());
                for (MavenArtifactNode artifactNode : mavenArtifactNode.getDependencies()) {
                    addArtifact(components, artifactNode.getArtifact());
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
        TreeModel issuesTree = new DefaultTreeModel(rootNode, false);
        for (MavenProject mavenProject : MavenProjectsManager.getInstance(project).getProjects()) {
            for (MavenArtifactNode dependencyTree : mavenProject.getDependencyTree()) {
                updateChildrenNodes(rootNode, dependencyTree);
            }
        }
        return issuesTree;
    }

    private void updateChildrenNodes(ScanTreeNode parentNode, MavenArtifactNode mavenArtifactNode) {
        ScanTreeNode currentNode = createArtifactNode(mavenArtifactNode.getArtifact());
        for (MavenArtifactNode childrenArifactNode : mavenArtifactNode.getDependencies()) {
            updateChildrenNodes(currentNode, childrenArifactNode);
        }
        parentNode.add(currentNode);
    }

    private ScanTreeNode createArtifactNode(MavenArtifact artifact) {
        ScanTreeNode scanTreeNode = new ScanTreeNode(artifact);
        Artifact scanArtifact = getArtifactSummary(artifact.getDisplayStringForLibraryName());
        if (scanArtifact != null) {
            scanTreeNode.setIssues(scanArtifact.issues);
            scanTreeNode.setLicenses(scanArtifact.licenses);
            scanTreeNode.setGeneralInfo(scanArtifact.general);
        }
        return scanTreeNode;
    }

    private String getArtifactChecksum(MavenArtifact artifact) {
        try {
            return calculateSha1(artifact.getFile());
        } catch (Exception e) {
            // Do nothing
        }
        return "";
    }

    /**
     * Maven project listener for scanning artifacts on dependencies changes.
     */
    private class MavenProjectsListene implements MavenProjectsManager.Listener {

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