package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.server.NativeMavenProjectHolder;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.Scope;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {

    MavenScanManager(Project project, ScanLogic logic) throws IOException {
        super(project, Utils.getProjectBasePath(project).toString(), ComponentPrefix.GAV, logic);
        getLog().info("Found Maven project: " + getProjectName());
        MavenProjectsManager.getInstance(project).addProjectsTreeListener(new MavenProjectsTreeListener());
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
    protected void buildTree(boolean shouldToast) {
        DependencyTree rootNode = new DependencyTree(project.getName());
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject -> populateMavenModule(rootNode, rootMavenProject, Sets.newHashSet()));
        GeneralInfo generalInfo = new GeneralInfo().artifactId(project.getName()).path(basePath).pkgType("maven");
        rootNode.setGeneralInfo(generalInfo);
        if (rootNode.getChildren().size() == 1) {
            setScanResults((DependencyTree) rootNode.getChildAt(0));
        } else {
            setScanResults(rootNode);
        }
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        // As project can contain sub-projects, look for all 'pom.xml' files under it.
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(project), XmlFileType.INSTANCE);
        return FilenameIndex.getFilesByName(project, "pom.xml", scope);
    }

    @Override
    protected LocalInspectionTool getInspectionTool() {
        return new MavenInspection();
    }

    private void addSubmodules(DependencyTree mavenNode, MavenProject mavenProject, Set<String> added) {
        mavenProject.getExistingModuleFiles().stream()
                .map(this::getModuleByVirtualFile)
                .filter(Objects::nonNull)
                .forEach(mavenModule -> populateMavenModule(mavenNode, mavenModule, added));
    }

    /**
     * Populate recursively the dependency tree with the maven module and its dependencies.
     *
     * @param root             - The root dependencies node
     * @param rootMavenProject - The root Maven project
     * @param added            - This set is used to make sure the dependencies added are unique between module and its parent
     */
    private void populateMavenModule(DependencyTree root, MavenProject rootMavenProject, Set<String> added) {
        DependencyTree mavenNode = populateMavenModuleNode(rootMavenProject);
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

    private void addMavenProjectDependencies(DependencyTree node, MavenProject mavenProject, Set<String> added) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(dependencyTree -> added.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(dependencyTree -> updateChildrenNodes(node, dependencyTree, added, true));
    }

    /**
     * Populate Maven module node.
     */
    private DependencyTree populateMavenModuleNode(MavenProject mavenProject) {
        DependencyTree node = new DependencyTree(mavenProject.getMavenId().getArtifactId());
        MavenId mavenId = mavenProject.getMavenId();
        node.setGeneralInfo(new GeneralInfo()
                .groupId(mavenId.getGroupId())
                .artifactId(mavenId.getArtifactId())
                .version(mavenId.getVersion())
                .pkgType("maven"));
        return node;
    }

    private void updateChildrenNodes(DependencyTree parentNode, MavenArtifactNode mavenArtifactNode, Set<String> added, boolean setScopes) {
        // This set is used to disallow duplications between a node and its ancestors
        final Set<String> addedInSubTree = Sets.newHashSet(added);
        MavenArtifact mavenArtifact = mavenArtifactNode.getArtifact();
        DependencyTree currentNode = new DependencyTree(mavenArtifact.getDisplayStringSimple());
        if (setScopes) {
            currentNode.setScopes(Sets.newHashSet(new Scope(mavenArtifact.getScope())));
        }
        populateDependencyTreeNode(currentNode);
        mavenArtifactNode.getDependencies()
                .stream()
                .filter(dependencyTree -> addedInSubTree.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode, addedInSubTree, false));
        parentNode.add(currentNode);
    }

    /**
     * Maven projects tree listener for scanning artifacts on dependencies changes.
     */

    private final class MavenProjectsTreeListener implements MavenProjectsTree.Listener {
        @Override
        public void projectResolved(@NotNull Pair<MavenProject, MavenProjectChanges> projectWithChanges,
                                    NativeMavenProjectHolder nativeMavenProject) {
            asyncScanAndUpdateResults();
        }
    }
}