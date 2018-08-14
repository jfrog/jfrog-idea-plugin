package org.jfrog.idea.xray.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.summary.Components;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.persistency.types.Artifact;
import org.jfrog.idea.xray.persistency.types.GeneralInfo;
import org.jfrog.idea.xray.persistency.types.License;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    public MavenScanManager() {
    }

    public MavenScanManager(Project project) {
        super(project);
        MavenProjectsManager.getInstance(project).addManagerListener(new MavenProjectsListener());
    }

    public static boolean isApplicable(@NotNull Project project) {
        return MavenProjectsManager.getInstance(project).hasProjects();
    }

    public Set<Path> getProjectPaths() {
        Set<Path> paths = super.getProjectPaths();
        MavenProjectsManager.getInstance(project).getProjects().forEach(mavenProject -> paths.add(Paths.get(mavenProject.getDirectory()).toAbsolutePath()));
        return paths;
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
        MavenProjectsManager.getInstance(project).getProjects().forEach(mavenProject ->
                mavenProject.getDependencyTree()
                        .stream()
                        .filter(mavenArtifactNode ->
                                added.add(mavenArtifactNode.getArtifact().getDisplayStringForLibraryName()))
                        .forEach(mavenArtifactNode -> {
                            addArtifact(components, mavenArtifactNode.getArtifact());
                            mavenArtifactNode.getDependencies().forEach(artifactNode ->
                                    addArtifact(components, artifactNode.getArtifact()));
                        }));
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
        // Any parent pom will appear in the dependencies tree. We want to display it as a module instead.
        Set<String> projects = Sets.newHashSet();
        MavenProjectsManager.getInstance(project).getProjects().forEach(project -> projects.add(project.getMavenId().getKey()));
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject ->
                populateMavenModule(rootNode, rootMavenProject, added, projects));
        return issuesTree;
    }

    private void addSubmodules(ScanTreeNode mavenNode, MavenProject mavenProject, Set<String> added, Set<String> projectsIds) {
        mavenProject.getExistingModuleFiles()
                .forEach(virtualFile -> {
                            MavenProject mavenModule = getModuleByVirtualFile(virtualFile);
                            if (mavenModule != null) {
                                populateMavenModule(mavenNode, mavenModule, added, projectsIds);
                            }
                        }
                );
    }

    private void populateMavenModule(ScanTreeNode rootNode, MavenProject rootMavenProject, Set<String> added, Set<String> projects) {
        ScanTreeNode mavenNode = populateScanTreeNode(rootMavenProject);
        rootNode.add(mavenNode);
        addMavenProjectDependencies(mavenNode, rootMavenProject, added, projects);
        addSubmodules(mavenNode, rootMavenProject, added, projects);
    }

    private MavenProject getModuleByVirtualFile(VirtualFile virtualFile) {
        return MavenProjectsManager.getInstance(project).getProjects()
                .stream()
                .filter(mavenModule -> Objects.equals(mavenModule.getFile().getCanonicalPath(), virtualFile.getCanonicalPath()))
                .findAny()
                .orElse(null);
    }

    private void addMavenProjectDependencies(ScanTreeNode node, MavenProject mavenProject, Set<String> added, Set<String> projectsIds) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(dependencyTree -> added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()) &&
                        !projectsIds.contains(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(dependencyTree -> updateChildrenNodes(node, dependencyTree));
    }

    /**
     * Populate root modules ScanTreeNode with issues, licenses and general info from the scan cache.
     */
    private ScanTreeNode populateScanTreeNode(MavenProject mavenProject) {
        ScanTreeNode node = new ScanTreeNode(mavenProject.getMavenId().getArtifactId(), true);
        node.setGeneralInfo(new GeneralInfo()
                .componentId(mavenProject.getMavenId().toString())
                .pkgType("maven"));
        Artifact scanArtifact = getArtifactSummary(mavenProject.getMavenId().getArtifactId());
        if (scanArtifact != null) {
            node.setLicenses(Sets.newHashSet(scanArtifact.licenses));
            return node;
        }
        node.setLicenses(new HashSet<>(Collections.singletonList(new License())));
        return node;
    }

    private void updateChildrenNodes(ScanTreeNode parentNode, MavenArtifactNode mavenArtifactNode) {
        ScanTreeNode currentNode = new ScanTreeNode(mavenArtifactNode.getArtifact().getDisplayStringForLibraryName());
        populateScanTreeNode(currentNode);
        mavenArtifactNode.getDependencies()
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode));
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