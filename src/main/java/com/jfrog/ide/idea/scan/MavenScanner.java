package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.inspections.AbstractInspection;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.menus.filtermanager.ConsistentFilterManager;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.model.MavenArtifactState;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
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
public class MavenScanner extends ScannerBase {
    private final String PKG_TYPE = "maven";
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
    protected DependencyTree buildTree() {
        DependencyTree rootNode = new DependencyTree(project.getName());
        rootNode.setMetadata(true);
        MavenProjectsManager.getInstance(project).getRootProjects().forEach(rootMavenProject -> populateMavenModule(rootNode, rootMavenProject));
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
    protected String getPackageManagerName() {
        return PKG_TYPE;
    }

    private void addSubmodules(DependencyTree mavenNode, MavenProject mavenProject) {
        mavenProject.getExistingModuleFiles().stream()
                .map(this::getModuleByVirtualFile)
                .filter(Objects::nonNull)
                .forEach(mavenModule -> populateMavenModule(mavenNode, mavenModule));
    }

    /**
     * Populate recursively the dependency tree with the maven module and its dependencies.
     *
     * @param root             - The root dependencies node
     * @param rootMavenProject - The root Maven project
     */
    private void populateMavenModule(DependencyTree root, MavenProject rootMavenProject) {
        DependencyTree mavenNode = populateMavenModuleNode(rootMavenProject);
        mavenNode.setMetadata(true);
        root.add(mavenNode);
        addMavenProjectDependencies(mavenNode, rootMavenProject);
        addSubmodules(mavenNode, rootMavenProject);
    }

    private MavenProject getModuleByVirtualFile(VirtualFile virtualFile) {
        return MavenProjectsManager.getInstance(project).getProjects()
                .stream()
                .filter(mavenModule -> Objects.equals(mavenModule.getFile().getCanonicalPath(), virtualFile.getCanonicalPath()))
                .findAny()
                .orElse(null);
    }

    private void addMavenProjectDependencies(DependencyTree node, MavenProject mavenProject) {
        mavenProject.getDependencyTree()
                .stream()
                .filter(mavenArtifactNode -> mavenArtifactNode.getState() == MavenArtifactState.ADDED)
                .forEach(mavenArtifactNode -> updateChildrenNodes(node, mavenArtifactNode, true));
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

    private void updateChildrenNodes(DependencyTree parentNode, MavenArtifactNode mavenArtifactNode, boolean setScopes) {
        MavenArtifact mavenArtifact = mavenArtifactNode.getArtifact();
        DependencyTree currentNode = new DependencyTree(mavenArtifact.getDisplayStringSimple());
        if (setScopes) {
            currentNode.setScopes(Sets.newHashSet(new Scope(mavenArtifact.getScope())));
        }
        mavenArtifactNode.getDependencies()
                .stream()
                .filter(mavenArtifactChild -> mavenArtifactChild.getState() == MavenArtifactState.ADDED)
                .forEach(childrenArtifactNode -> updateChildrenNodes(currentNode, childrenArtifactNode, false));
        parentNode.add(currentNode);
    }

    /**
     * Groups a collection of DependencyNodes by the descriptor files of the modules that depend on them.
     * The returned DependencyNodes inside the FileTreeNodes are clones of the ones in depScanResults.
     *
     * @param depScanResults - collection of DependencyNodes.
     * @param depMap         - a map of DependencyTree objects by their component ID.
     * @return A list of FileTreeNodes (that are all DescriptorFileTreeNodes) having the DependencyNodes as their children.
     */
    @Override
    protected List<FileTreeNode> groupDependenciesToDescriptorNodes(Collection<DependencyNode> depScanResults, Map<String, List<DependencyTree>> depMap) {
        Map<String, DescriptorFileTreeNode> descriptorMap = new HashMap<>();
        for (DependencyNode dependencyNode : depScanResults) {
            Map<String, Boolean> addedDescriptors = new HashMap<>();
            for (DependencyTree dep : depMap.get(dependencyNode.getComponentId())) {
                DependencyTree currDep = dep;
                while (currDep != null) {
                    if (currDep.getGeneralInfo() != null) {
                        String pomPath = currDep.getGeneralInfo().getPath();
                        if (StringUtils.endsWith(pomPath, POM_FILE_NAME) && !addedDescriptors.containsKey(pomPath)) {
                            descriptorMap.putIfAbsent(pomPath, new DescriptorFileTreeNode(pomPath));

                            // Each dependency might be a child of more than one POM file, but Artifact is a tree node, so it can have only one parent.
                            // The solution for this is to clone the dependency before adding it as a child of the POM.
                            DependencyNode clonedDep = (DependencyNode) dependencyNode.clone();
                            clonedDep.setIndirect(dep.getParent() != currDep);
                            descriptorMap.get(pomPath).addDependency(clonedDep);
                            addedDescriptors.put(pomPath, true);
                        }
                    }
                    currDep = (DependencyTree) currDep.getParent();
                }
            }
        }
        return new ArrayList<>(descriptorMap.values());
    }

    @Override
    public String getCodeBaseLanguage() {
        return "java";
    }
}
