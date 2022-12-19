package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.tree.Artifact;
import com.jfrog.ide.common.tree.DescriptorFileTreeNode;
import com.jfrog.ide.common.tree.FileTreeNode;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanManager extends ScanManager {
    private final String PKG_TYPE = "maven";
    private final String POM_FILE_NAME = "pom.xml";

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param executor - An executor that should limit the number of running tasks to 3
     */
    MavenScanManager(Project project, ExecutorService executor) {
        super(project, Utils.getProjectBasePath(project).toString(), ComponentPrefix.GAV, executor);
        getLog().info("Found Maven project: " + getProjectName());
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
    protected DependencyTree buildTree(boolean shouldToast) {
        DependencyTree rootNode = new DependencyTree(project.getName());
        rootNode.setMetadata(true);
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject -> populateMavenModule(rootNode, rootMavenProject, Sets.newHashSet()));
        GeneralInfo generalInfo = new GeneralInfo().componentId(project.getName()).path(basePath).pkgType(PKG_TYPE);
        rootNode.setGeneralInfo(generalInfo);
        if (rootNode.getChildren().size() == 1) {
            return (DependencyTree) rootNode.getChildAt(0);
        }
        return rootNode;
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        // As project can contain sub-projects, look for all 'pom.xml' files under it.
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(project), XmlFileType.INSTANCE);
        Collection<VirtualFile> allPoms = FilenameIndex.getVirtualFilesByName("pom.xml", scope);
        PsiManager psiManager = PsiManager.getInstance(project);
        return allPoms.stream().map(psiManager::findFile).toArray(PsiFile[]::new);
    }

    @Override
    protected AbstractInspection getInspectionTool() {
        return new MavenInspection();
    }

    @Override
    protected String getProjectPackageType() {
        return PKG_TYPE;
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
        mavenNode.setMetadata(true);
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
        node.setGeneralInfo(new GeneralInfo().pkgType("maven")
                .componentId(createComponentId(mavenId.getGroupId(), mavenId.getArtifactId(), mavenId.getVersion()))
                .path(mavenProject.getPath()));

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
        // TODO: remove?
//        populateDependencyTreeNode(currentNode);
        mavenArtifactNode.getDependencies()
                .stream()
                .filter(dependencyTree -> addedInSubTree.add(dependencyTree.getArtifact().getDisplayStringForLibraryName()))
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode, addedInSubTree, false));
        parentNode.add(currentNode);
    }

    @Override
    protected List<FileTreeNode> groupArtifactsToDescriptorNodes(Collection<Artifact> depScanResults, Map<String, List<DependencyTree>> depMap) {
        Map<String, DescriptorFileTreeNode> treeNodeMap = new HashMap<>();
        for (Artifact artifact : depScanResults) {
            for (DependencyTree dep : depMap.get(artifact.getGeneralInfo().getComponentId())) {
                DependencyTree currDep = dep;
                while (currDep != null) {
                    if (currDep.getGeneralInfo() != null) {
                        String pomPath = currDep.getGeneralInfo().getPath();
                        if (pomPath != null && pomPath.endsWith(POM_FILE_NAME)) {
                            if (!treeNodeMap.containsKey(currDep.getGeneralInfo().getPath())) {
                                treeNodeMap.put(pomPath, new DescriptorFileTreeNode(pomPath));
                            }
                            treeNodeMap.get(pomPath).addDependency(artifact);
                        }
                    }
                    currDep = (DependencyTree) currDep.getParent();
                }
            }
        }
        return new ArrayList<>(treeNodeMap.values());
    }
}
