package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.build.extractor.scan.Artifact;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    MavenScanManager(Project project) throws IOException {
        super(project, ComponentPrefix.GAV);
        MavenProjectsManager.getInstance(project).addManagerListener(new MavenProjectsListener());
    }

    static boolean isApplicable(@NotNull Project project) {
        return MavenProjectsManager.getInstance(project).hasProjects();
    }

    /**
     * Returns all project modules locations as Paths.
     * Other scanners such as npm will use this paths in order to find modules.
     *
     * @return all project modules locations as Paths
     */
    public Set<Path> getProjectPaths() {
        Set<Path> paths = super.getProjectPaths();
        MavenProjectsManager.getInstance(project).getProjects().forEach(mavenProject -> paths.add(Paths.get(mavenProject.getDirectory())));
        return paths;
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) {
        DependenciesTree rootNode = new DependenciesTree(project.getName());
        // This set is used to make sure the artifacts added are unique
        Set<String> added = Sets.newHashSet();
        // Any parent pom will appear in the dependencies tree. We want to display it as a module instead.
        Set<String> projects = Sets.newHashSet();
        MavenProjectsManager.getInstance(project).getProjects().forEach(project -> projects.add(project.getMavenId().getKey()));
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject ->
                populateMavenModule(rootNode, rootMavenProject, added, projects));
        GeneralInfo generalInfo = new GeneralInfo(project.getName(), project.getName(), Utils.getProjectBasePath(project).toString(), "maven");
        rootNode.setGeneralInfo(generalInfo);
        if (rootNode.getChildren().size() == 1) {
            setScanResults((DependenciesTree) rootNode.getChildAt(0));
        } else {
            setScanResults(rootNode);
        }
    }

    private void addSubmodules(DependenciesTree mavenNode, MavenProject mavenProject, Set<String> added, Set<String> projectsIds) {
        mavenProject.getExistingModuleFiles()
                .forEach(virtualFile -> {
                            MavenProject mavenModule = getModuleByVirtualFile(virtualFile);
                            if (mavenModule != null) {
                                populateMavenModule(mavenNode, mavenModule, added, projectsIds);
                            }
                        }
                );
    }

    private void populateMavenModule(DependenciesTree rootNode, MavenProject rootMavenProject, Set<String> added, Set<String> projects) {
        DependenciesTree mavenNode = populateScanTreeNode(rootMavenProject);
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

    private void addMavenProjectDependencies(DependenciesTree node, MavenProject mavenProject, Set<String> added, Set<String> projectsIds) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(dependencyTree -> added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()) &&
                        !projectsIds.contains(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(dependencyTree -> updateChildrenNodes(node, dependencyTree));
    }

    /**
     * Populate root modules ScanTreeNode with issues, licenses and general info from the scan cache.
     */
    private DependenciesTree populateScanTreeNode(MavenProject mavenProject) {
        DependenciesTree node = new DependenciesTree(mavenProject.getMavenId().getArtifactId());
        node.setGeneralInfo(new GeneralInfo()
                .componentId(mavenProject.getMavenId().toString())
                .pkgType("maven"));
        Artifact scanArtifact = getArtifactSummary(mavenProject.getMavenId().getArtifactId());
        if (scanArtifact != null) {
            node.setLicenses(Sets.newHashSet(scanArtifact.getLicenses()));
            return node;
        }
        return node;
    }

    private void updateChildrenNodes(DependenciesTree parentNode, MavenArtifactNode mavenArtifactNode) {
        DependenciesTree currentNode = new DependenciesTree(mavenArtifactNode.getArtifact().getDisplayStringForLibraryName());
        populateDependenciesTreeNode(currentNode);
        mavenArtifactNode.getDependencies()
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode));
        parentNode.add(currentNode);
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