package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    MavenScanManager(Project project) throws IOException {
        super(project, project, ComponentPrefix.GAV);
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
        return MavenProjectsManager.getInstance(project).getProjects().stream()
                .map(MavenProject::getDirectory)
                .map(Paths::get)
                .collect(Collectors.toSet());
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) {
        DependenciesTree rootNode = new DependenciesTree(project.getName());
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject -> populateMavenModule(rootNode, rootMavenProject, Sets.newHashSet()));
        GeneralInfo generalInfo = new GeneralInfo().artifactId(project.getName()).path(Utils.getProjectBasePath(project).toString()).pkgType("maven");
        rootNode.setGeneralInfo(generalInfo);
        if (rootNode.getChildren().size() == 1) {
            setScanResults((DependenciesTree) rootNode.getChildAt(0));
        } else {
            setScanResults(rootNode);
        }
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        // As project can contain sub-projects, look for all 'pom.xml' files under it.
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(project), StdFileTypes.XML);
        return FilenameIndex.getFilesByName(project, "pom.xml", scope);
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new MavenInspection();
    }

    private void addSubmodules(DependenciesTree mavenNode, MavenProject mavenProject, Set<String> added) {
        mavenProject.getExistingModuleFiles().stream()
                .map(this::getModuleByVirtualFile)
                .filter(Objects::nonNull)
                .forEach(mavenModule -> populateMavenModule(mavenNode, mavenModule, added));
    }

    /**
     * Populate recursively the dependencies tree with the maven module and its dependencies.
     *
     * @param root             - The root dependencies node
     * @param rootMavenProject - The root Maven project
     * @param added            - This set is used to make sure the dependencies added are unique between module and its parent
     */
    private void populateMavenModule(DependenciesTree root, MavenProject rootMavenProject, Set<String> added) {
        DependenciesTree mavenNode = populateMavenModuleNode(rootMavenProject);
        root.add(mavenNode);
        added = Sets.newHashSet(added);
        added.add(rootMavenProject.toString());
        addMavenProjectDependencies(mavenNode, rootMavenProject, added);
        addSubmodules(mavenNode, rootMavenProject, added);
    }

    private MavenProject getModuleByVirtualFile(VirtualFile virtualFile) {
        return MavenProjectsManager.getInstance(project).getProjects()
                .stream()
                .filter(mavenModule -> Objects.equals(mavenModule.getFile().getCanonicalPath(), virtualFile.getCanonicalPath()))
                .findAny()
                .orElse(null);
    }

    private void addMavenProjectDependencies(DependenciesTree node, MavenProject mavenProject, Set<String> added) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(dependencyTree -> added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(dependencyTree -> updateChildrenNodes(node, dependencyTree, added));
    }

    /**
     * Populate Maven module node.
     */
    private DependenciesTree populateMavenModuleNode(MavenProject mavenProject) {
        DependenciesTree node = new DependenciesTree(mavenProject.getMavenId().getArtifactId());
        MavenId mavenId = mavenProject.getMavenId();
        node.setGeneralInfo(new GeneralInfo()
                .groupId(mavenId.getGroupId())
                .artifactId(mavenId.getArtifactId())
                .version(mavenId.getVersion())
                .pkgType("maven"));
        return node;
    }

    private void updateChildrenNodes(DependenciesTree parentNode, MavenArtifactNode mavenArtifactNode, Set<String> added) {
        DependenciesTree currentNode = new DependenciesTree(mavenArtifactNode.getArtifact().getDisplayStringSimple());
        populateDependenciesTreeNode(currentNode);
        mavenArtifactNode.getDependencies()
                .stream()
                .filter(dependencyTree -> added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode, added));
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
            asyncScanAndUpdateResults();
        }
    }
}