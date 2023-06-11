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
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.scan.data.PackageType;
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
    protected PackageType getPackageManagerType() {
        return PackageType.MAVEN;
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
}
