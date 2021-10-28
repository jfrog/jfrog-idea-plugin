package com.jfrog.ide.idea.inspections;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parent class of all inspections and annotations.
 * The inspections are the "Show in dependency tree" action.
 * The annotations are the "Top issue" and "Licenses" labels.
 *
 * @author yahavi
 */
public abstract class AbstractInspection extends LocalInspectionTool implements Annotator {

    private final String packageDescriptorName;

    AbstractInspection(String packageDescriptorName) {
        this.packageDescriptorName = packageDescriptorName;
    }

    /**
     * Get Psi element and decide whether to add "Show in dependency tree" option, and register a corresponding
     * navigation from item in tree to item in project-descriptor.
     *
     * @param problemsHolder - The "Show in dependency tree" option will be registered in this container.
     * @param element        - The Psi element in the package descriptor
     */
    void visitElement(ProblemsHolder problemsHolder, PsiElement element) {
        List<DependencyTree> dependencies = getDependencies(element);
        if (CollectionUtils.isEmpty(dependencies)) {
            return;
        }
        NavigationService navigationService = NavigationService.getInstance(element.getProject());
        for (DependencyTree dependency : dependencies) {
            InspectionUtils.registerProblem(problemsHolder, dependency, getTargetElements(element), dependencies.size());
            navigationService.addNavigation(dependency, element);
        }
    }

    /**
     * Get Psi element and decide whether to add licenses and top issue annotations.
     *
     * @param annotationHolder - The annotations will be registered in this container
     * @param element          - The Psi element in the package descriptor
     */
    void visitElement(AnnotationHolder annotationHolder, PsiElement element) {
        List<DependencyTree> dependencies = getDependencies(element);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            AnnotationUtils.registerAnnotation(annotationHolder, dependencies.get(0), getTargetElements(element));
        }
    }

    /**
     * Get the elements to apply the inspections and annotations.
     *
     * @param element - The Psi element in the package descriptor
     * @return elements array to apply the inspection and annotation
     */
    abstract PsiElement[] getTargetElements(PsiElement element);

    /**
     * Get the relevant scan manager according to the project type and path.
     *
     * @param project - The Project
     * @param path    - Path to project
     * @return ScanManager
     */
    abstract ScanManager getScanManager(Project project, String path);

    /**
     * Return true if and only if the Psi element is a dependency.
     *
     * @param element - The Psi element in the package descriptor.
     * @return true if and only if the Psi element is a dependency
     */
    abstract boolean isDependency(PsiElement element);

    /**
     * Create general info from the Psi element. Called when isDependency(element) == true.
     *
     * @param element - The Psi element in the package descriptor
     * @return GeneralInfo
     */
    abstract String createComponentName(PsiElement element);

    /**
     * Get the submodules containing the dependency in the Psi element. The result depends on the root project structure:
     * In case of dependency selected:
     * Single project, single module - Return the root project.
     * Single project, multi module - Return the module contains the dependency.
     * Multi project - Return the module containing the dependency within the projects.
     *
     * @param element       - The Psi element in the package descriptor
     * @param componentName - Component name represents a dependency without version
     * @return Set of modules containing the dependency or null if not found
     */
    abstract Set<DependencyTree> getModules(PsiElement element, String componentName);

    /**
     * Determine whether to apply the inspection on the Psi element.
     *
     * @param element - The Psi element in the package descriptor
     * @return true if and only if the element is a dependency and the plugin is ready to show inspection for it
     */
    boolean isShowInspection(PsiElement element) {
        Project project = element.getProject();

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JFrog");
        if (toolWindow == null) {
            return false; // Tool window not yet activated
        }

        VirtualFile editorFile = element.getContainingFile().getVirtualFile();
        if (editorFile == null || editorFile.getParent() == null || !editorFile.getPath().endsWith(packageDescriptorName)) {
            return false; // File is not a package descriptor file
        }

        ScanManager scanManager = getScanManager(project, editorFile.getParent().getPath());
        if (scanManager == null) {
            return false; // Scan manager for this project not yet created
        }
        return isDependency(element);
    }

    /**
     * Get all dependencies in the dependency tree that relevant to the element.
     *
     * @param element - The Psi element in the package descriptor
     * @return all dependencies in the dependency tree that relevant to the element
     */
    List<DependencyTree> getDependencies(PsiElement element) {
        if (!isShowInspection(element)) {
            return null; // Inspection is not needed for this element
        }
        String componentName = createComponentName(element);
        if (componentName == null) {
            return null; // Creating the component name failed
        }
        Set<DependencyTree> modules = getModules(element, componentName);
        if (modules == null) {
            return null; // No modules found for this element
        }
        return modules.stream()
                .map(module -> getModuleDependency(module, componentName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get the module dependency that matches to the input general info.
     *
     * @param module        - The dependency tree module
     * @param componentName - Component name represents a dependency without version
     * @return module dependencies that match to the input general info
     */
    private DependencyTree getModuleDependency(DependencyTree module, String componentName) {
        for (DependencyTree dependency : module.getChildren()) {
            GeneralInfo childGeneralInfo = dependency.getGeneralInfo();
            if (childGeneralInfo == null) {
                childGeneralInfo = new GeneralInfo().componentId(dependency.getUserObject().toString());
            }
            if (compareGeneralInfo(childGeneralInfo, componentName)) {
                return dependency;
            }
        }
        return null;
    }

    /**
     * Search for the node of the project in the dependency tree. If this is a single project, return the root.
     *
     * @param root    - The root of the dependency tree
     * @param project - The project
     * @return the node of the project in the dependency tree
     */
    DependencyTree getProjectNode(DependencyTree root, Project project) {
        if (root.getGeneralInfo() != null) {
            return root;
        }
        return root.getChildren().stream()
                .filter(child -> StringUtils.equals((String) child.getUserObject(), project.getName()))
                .findAny()
                .orElse(root);
    }

    /**
     * Get the root of the dependency tree of the input project.
     *
     * @param element - The Psi element in the package descriptor
     * @return root of the dependency tree of the input project
     */
    DependencyTree getRootDependencyTree(PsiElement element) {
        Project project = element.getProject();
        ComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        if (componentsTree == null || componentsTree.getModel() == null) {
            return null;
        }
        return (DependencyTree) componentsTree.getModel().getRoot();
    }

    /**
     * Return true if and only if the dependency tree node containing the input path.
     *
     * @param node - The dependency tree node
     * @param path - The path to check
     * @return true if and only if the dependency tree node containing the input path
     */
    private boolean isContainingPath(DependencyTree node, String path) {
        GeneralInfo childGeneralInfo = node.getGeneralInfo();
        return StringUtils.equals(childGeneralInfo.getPath(), path);
    }

    /**
     * Get all submodules from the dependencies-tree which are containing the dependency element.
     * Currently in use for Go and npm.
     *
     * @param root    - The root of the dependency tree
     * @param element - The Psi element in the package descriptor
     * @return Set of modules containing the dependency or null if not found
     */
    Set<DependencyTree> collectModules(DependencyTree root, PsiElement element) {
        // Single project, single module
        if (root.getGeneralInfo() != null) {
            return Sets.newHashSet(root);
        }

        // Multi project
        String path = element.getContainingFile().getVirtualFile().getParent().getPath();
        for (DependencyTree child : root.getChildren()) {
            if (isContainingPath(child, path)) {
                return Sets.newHashSet(child);
            }
        }
        return null;
    }

    /**
     * Get all submodules from the dependencies-tree which are containing the dependency element.
     * Currently in use for Gradle and Maven.
     *
     * @param root          - The root of the dependency tree
     * @param project       - The project
     * @param modulesList   - List of all relevant modules
     * @param componentName - Component name represents a dependency without version
     * @return set of all modules containing the dependency stated in the general info
     */
    Set<DependencyTree> collectModules(DependencyTree root, Project project, List<?> modulesList, String componentName) {
        // Single project, single module
        if (modulesList.size() <= 1 && root.getGeneralInfo() != null) {
            return Sets.newHashSet(root);
        }

        // Get the node of the project
        root = getProjectNode(root, project);

        // Multi modules
        return collectMultiModules(root, componentName);
    }

    /**
     * Collect the nodes containing the dependency. Specifically handle the following cases:
     * 1. The dependency is a module under the project node
     * 2. The dependency is a direct dependency under the project node
     * 3. The dependency is a level 2 dependency under the project node
     *
     * @param projectNode   - The project node
     * @param componentName - Component name represents a dependency without version
     * @return the nodes containing the dependency
     */
    private Set<DependencyTree> collectMultiModules(DependencyTree projectNode, String componentName) {
        Set<DependencyTree> modules = Sets.newHashSet();
        for (DependencyTree child : projectNode.getChildren()) {
            Object userObject = child.getUserObject();
            if (userObject == null) {
                continue;
            }
            String componentId = userObject.toString();
            if (componentId.contains(":")) {
                // Check if the dependency is a direct dependency under the project node
                GeneralInfo generalInfo = new GeneralInfo().componentId(componentId);
                if (compareGeneralInfo(generalInfo, componentName)) {
                    modules.add(projectNode);
                }
            } else {
                // Check if the dependency is a module under the project node
                if (componentId.equals(componentName)) {
                    modules.add(projectNode);
                }
            }

            // Check if the dependency is a level 2 dependency under the project node
            if (isParent(child, componentName)) {
                modules.add(child);
            }
        }
        return modules;
    }

    /**
     * Return true iff the node is a direct parent of the dependency.
     *
     * @param node          - The project node
     * @param componentName - Component name represents a dependency without version
     * @return true iff the node is a direct parent of the dependency
     */
    private boolean isParent(DependencyTree node, String componentName) {
        return node.getChildren().stream()
                .map(DependencyTree::getGeneralInfo)
                .filter(Objects::nonNull)
                .anyMatch(childGeneralInfo -> compareGeneralInfo(childGeneralInfo, componentName));
    }

    /**
     * Compare the generated general info from the Psi element and the build info from the Dependency tree.
     *
     * @param generalInfo   - General info from the dependency tree
     * @param componentName - Component name represents a dependency without version
     * @return true if the general info matches the component name
     */
    boolean compareGeneralInfo(GeneralInfo generalInfo, String componentName) {
        // Module name
        if (StringUtils.countMatches(componentName, ":") == 0) {
            return StringUtils.equals(generalInfo.getArtifactId(), componentName);
        }
        // Dependency
        String childComponentId = StringUtils.substringBeforeLast(generalInfo.getComponentId(), ":");
        return StringUtils.equals(componentName, childComponentId);
    }
}
