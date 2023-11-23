package com.jfrog.ide.idea.scan;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.deptree.DepTreeNode;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.scan.data.PackageManagerType;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * Created by romang on 3/2/17.
 */
public class MavenScanner extends ScannerBase {
    private final String POM_FILE_NAME = "pom.xml";

    /**
     * @param project  - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                 like {@link ConsistentFilterManager} and {@link ComponentsTree}.
     * @param executor - An executor that should limit the number of running tasks to 3
     */
    MavenScanner(Project project, ExecutorService executor, ScanLogic scanLogic) {
        super(project, Utils.getProjectBasePath(project).toString(), ComponentPrefix.GAV, executor, scanLogic);
        getLog().info("Found Maven project: " + getProjectPath());
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
    protected DepTree buildTree() {
        String rootId = project.getName();
        DepTreeNode rootNode = new DepTreeNode();
        Map<String, DepTreeNode> nodes = new HashMap<>();
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject -> populateMavenModule(nodes, rootNode, rootMavenProject));
        if (rootNode.getChildren().size() == 1) {
            return new DepTree(rootNode.getChildren().iterator().next(), nodes);
        }
        nodes.put(rootId, rootNode);
        return new DepTree(rootId, nodes);
    }

    @Override
    protected PsiFile[] getProjectDescriptors() {
        // As project can contain subprojects, look for all 'pom.xml' files under it.
        GlobalSearchScope scope = GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(project), XmlFileType.INSTANCE);
        Collection<VirtualFile> allPoms = FilenameIndex.getVirtualFilesByName(POM_FILE_NAME, scope);
        PsiManager psiManager = PsiManager.getInstance(project);
        return allPoms.stream().map(psiManager::findFile).toArray(PsiFile[]::new);
    }

    @Override
    protected AbstractInspection getInspectionTool() {
        return new MavenInspection();
    }

    @Override
    protected PackageManagerType getPackageManagerType() {
        return PackageManagerType.MAVEN;
    }

    /**
     * Populate recursively the dependency tree with the maven module and its dependencies.
     *
     * @param nodes        a map of {@link DepTreeNode}s by their component IDs to be filled with the module's components.
     * @param parentModule the parent dependency node
     * @param mavenProject the root Maven project
     */
    private void populateMavenModule(Map<String, DepTreeNode> nodes, DepTreeNode parentModule, MavenProject mavenProject) {
        MavenId mavenId = mavenProject.getMavenId();
        String compId = createComponentId(mavenId.getGroupId(), mavenId.getArtifactId(), mavenId.getVersion());
        DepTreeNode mavenNode = getOrCreateMavenModuleNode(nodes, compId, mavenProject);
        parentModule.getChildren().add(compId);
        addMavenProjectDependencies(nodes, mavenNode, mavenProject);
        mavenProject.getExistingModuleFiles().stream()
                .map(this::getModuleByVirtualFile)
                .filter(Objects::nonNull)
                .forEach(mavenModule -> populateMavenModule(nodes, mavenNode, mavenModule));
    }

    private MavenProject getModuleByVirtualFile(VirtualFile virtualFile) {
        return MavenProjectsManager.getInstance(project).getProjects()
                .stream()
                .filter(mavenModule -> Objects.equals(mavenModule.getFile().getCanonicalPath(), virtualFile.getCanonicalPath()))
                .findAny()
                .orElse(null);
    }

    private void addMavenProjectDependencies(Map<String, DepTreeNode> nodes, DepTreeNode moduleNode, MavenProject mavenProject) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(mavenArtifactNode -> mavenArtifactNode.getState() == MavenArtifactState.ADDED)
                .forEach(mavenArtifactNode -> updateChildrenNodes(nodes, moduleNode, mavenArtifactNode, true));
    }

    private DepTreeNode getOrCreateMavenModuleNode(Map<String, DepTreeNode> nodes, String moduleCompId, MavenProject mavenProject) {
        if (!nodes.containsKey(moduleCompId)) {
            nodes.put(moduleCompId, new DepTreeNode());
        }
        return nodes.get(moduleCompId).descriptorFilePath(mavenProject.getPath());
    }

    private void updateChildrenNodes(Map<String, DepTreeNode> nodes, DepTreeNode parentNode, MavenArtifactNode mavenArtifactNode, boolean setScopes) {
        MavenArtifact mavenArtifact = mavenArtifactNode.getArtifact();
        String compId = mavenArtifact.getDisplayStringSimple();
        DepTreeNode currentNode;
        if (nodes.containsKey(compId)) {
            currentNode = nodes.get(compId);
        } else {
            currentNode = new DepTreeNode();
            nodes.put(compId, currentNode);
        }
        if (setScopes) {
            currentNode.getScopes().add(mavenArtifact.getScope());
        }
        mavenArtifactNode.getDependencies()
                .stream()
                .filter(mavenArtifactChild -> mavenArtifactChild.getState() == MavenArtifactState.ADDED)
                .forEach(childrenArtifactNode -> updateChildrenNodes(nodes, currentNode, childrenArtifactNode, false));
        parentNode.getChildren().add(compId);
    }

    /**
     * Groups a collection of {@link DependencyNode}s by the descriptor files of the modules that depend on them.
     * The returned DependencyNodes inside the {@link FileTreeNode}s are clones of the ones in depScanResults.
     *
     * @param depScanResults collection of DependencyNodes
     * @param depTree        the project's dependency tree
     * @param parents        a map of components by their IDs and their parents in the dependency tree
     * @return a list of FileTreeNodes (that are all DescriptorFileTreeNodes) having the DependencyNodes as their children
     */
    @Override
    protected List<FileTreeNode> groupDependenciesToDescriptorNodes(Collection<DependencyNode> depScanResults, DepTree depTree, Map<String, Set<String>> parents) {
        Map<String, DescriptorFileTreeNode> descriptorMap = new HashMap<>();
        Map<String, Set<String>> visitedComponents = new HashMap<>();
        for (DependencyNode dependencyNode : depScanResults) {
            String vulnerableDepId = dependencyNode.getComponentIdWithoutPrefix();
            Set<String> affectedModulesIds = getDependentModules(vulnerableDepId, depTree, parents, visitedComponents);
            for (String descriptorId : affectedModulesIds) {
                String descriptorPath = depTree.getNodes().get(descriptorId).getDescriptorFilePath();
                descriptorMap.putIfAbsent(descriptorPath, new DescriptorFileTreeNode(descriptorPath));

                // Each dependency might be a child of more than one POM file, but Artifact is a tree node, so it can have only one parent.
                // The solution for this is to clone the dependency before adding it as a child of the POM.
                DependencyNode clonedDep = (DependencyNode) dependencyNode.clone();
                clonedDep.setIndirect(!parents.get(vulnerableDepId).contains(descriptorId));
                descriptorMap.get(descriptorPath).addDependency(clonedDep);
            }
        }
        return new CopyOnWriteArrayList<>(descriptorMap.values());
    }

    /**
     * Retrieve component IDs of all modules in the project that are dependent on the specified component.
     *
     * @param compId            the component ID to identify modules depending on it
     * @param depTree           the project's dependency tree
     * @param parents           a map of components by their IDs and their parents in the dependency tree
     * @param visitedComponents a map of components for which dependent modules have already been found
     * @return a set of component IDs representing modules dependent on the specified component
     */
    Set<String> getDependentModules(String compId, DepTree depTree, Map<String, Set<String>> parents, Map<String, Set<String>> visitedComponents) {
        if (visitedComponents.containsKey(compId)) {
            return visitedComponents.get(compId);
        }
        Set<String> modulesIds = new HashSet<>();
        if (depTree.getNodes().get(compId).getDescriptorFilePath() != null) {
            modulesIds.add(compId);
        }
        if (parents.containsKey(compId)) {
            for (String parentId : parents.get(compId)) {
                modulesIds.addAll(getDependentModules(parentId, depTree, parents, visitedComponents));
            }
        }
        visitedComponents.put(compId, modulesIds);
        return modulesIds;
    }
}
