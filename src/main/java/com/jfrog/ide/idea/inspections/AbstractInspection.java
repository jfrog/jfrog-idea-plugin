package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.common.tree.BaseTreeNode;
import com.jfrog.ide.common.tree.DependencyNode;
import com.jfrog.ide.common.tree.DescriptorFileTreeNode;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.util.*;
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
    // True if the code inspection was automatically triggered after an Xray scan using InspectionEngine.runInspectionOnFile(...).
    private boolean afterScan;

    AbstractInspection(String packageDescriptorName) {
        this.packageDescriptorName = packageDescriptorName;
    }

    public void setAfterScan(boolean afterScan) {
        this.afterScan = afterScan;
    }

    /**
     * Get Psi element and decide whether to add "Show in dependency tree" option, and register a corresponding
     * navigation from item in tree to item in project-descriptor.
     *
     * @param problemsHolder - The "Show in dependency tree" option will be registered in this container.
     * @param element        - The Psi element in the package descriptor
     * @param isOnTheFly     - True if the inspection was triggered by opening a package descriptor file.
     *                       False if the inspection was triggered manually by clicking on "Code | Inspect Code".
     */
    void visitElement(ProblemsHolder problemsHolder, PsiElement element, boolean isOnTheFly) {
        if (!afterScan && !isOnTheFly) {
            // Code inspection was triggered manually by clicking on "Code | Inspect Code".
            return;
        }
        List<DependencyNode> dependencies = getDependencies(element);
        if (CollectionUtils.isEmpty(dependencies)) {
            return;
        }
        NavigationService navigationService = NavigationService.getInstance(element.getProject());
        for (DependencyNode dependency : dependencies) {
            if (isOnTheFly) {
                InspectionUtils.registerProblem(problemsHolder, dependency, getTargetElements(element), dependencies.size());
            }
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
        List<DependencyNode> dependencies = getDependencies(element);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            AnnotationUtils.registerAnnotation(annotationHolder, dependencies.get(0), getTargetElements(element), showAnnotationIcon(element));
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
     * Create a component name from the Psi element. Called when isDependency(element) == true.
     *
     * @param element - The Psi element in the package descriptor
     * @return GeneralInfo
     */
    abstract String createComponentName(PsiElement element);

    /**
     * Get the file descriptors nodes that containing the dependency in the Psi element.
     *
     * @param element - The Psi element in the package descriptor
     * @return Set of file nodes containing the dependency or null if not found
     */
    Set<DescriptorFileTreeNode> getFileDescriptors(PsiElement element) {
        Project project = element.getProject();
        ComponentsTree componentsTree = LocalComponentsTree.getInstance(project);
        if (componentsTree == null || componentsTree.getModel() == null) {
            return null;
        }
        Set<DescriptorFileTreeNode> fileDescriptors = new HashSet<>();
        Enumeration<TreeNode> roots = ((BaseTreeNode) componentsTree.getModel().getRoot()).children();
        for (TreeNode root : Collections.list(roots)) {
            if (root instanceof DescriptorFileTreeNode) {
                fileDescriptors.add((DescriptorFileTreeNode) root);
            }
        }
        return fileDescriptors;

    }

    /**
     * Override this method to determine whether to display multiple annotation icons in the same line.
     *
     * @param element - The Psi element in the package descriptor
     * @return true if it should show annotation icon.
     */
    boolean showAnnotationIcon(PsiElement element) {
        return true;
    }

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
     * Get all dependencies in the tree that relevant to the element.
     *
     * @param element - The Psi element in the package descriptor
     * @return all dependencies in the dependency tree that relevant to the element
     */
    List<DependencyNode> getDependencies(PsiElement element) {
        if (!isShowInspection(element)) {
            return null; // Inspection is not needed for this element
        }
        String componentName = createComponentName(element);
        if (componentName == null) {
            return null; // Failed creating the component name
        }
        Set<DescriptorFileTreeNode> filesNodes = getFileDescriptors(element);
        if (filesNodes == null) {
            return null; // No files descriptors found for this element
        }
        return filesNodes.stream().map(module -> getModuleDependency(module, componentName)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Get the module dependency that matches to the input general info.
     *
     * @param file          - The Descriptor file node in the tree
     * @param componentName - Component name representing a dependency without version
     * @return the dependency node that match to the input general info
     */
    private DependencyNode getModuleDependency(DescriptorFileTreeNode file, String componentName) {
        for (DependencyNode dependency : file.getDependencies()) {
            if (CompareDependencyNode(dependency, componentName)) {
                return dependency;
            }
        }
        return null;
    }

    /**
     * Compare the component name from the Psi element and the dependency node from the Dependency tree.
     *
     * @param node          - the dependency node from the dependency tree
     * @param componentName - Component name representing a dependency without version
     * @return true if the node matches the component name
     */
    boolean CompareDependencyNode(DependencyNode node, String componentName) {
        String artifactID = node.getGeneralInfo().getComponentIdWithoutPrefix();
        String impactPath = node.getImpactPathsString();
        if (StringUtils.countMatches(componentName, ":") == 0) {
            return StringUtils.equals(artifactID, componentName) || impactPath.contains(componentName);
        }
        String childComponentId = StringUtils.substringBeforeLast(artifactID, ":");
        return StringUtils.equals(componentName, childComponentId) || impactPath.contains(componentName);
    }
}
