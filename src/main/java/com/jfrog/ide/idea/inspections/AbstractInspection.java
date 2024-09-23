package com.jfrog.ide.idea.inspections;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.DescriptorFileTreeNode;
import com.jfrog.ide.common.nodes.SortableChildrenTreeNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.common.nodes.subentities.ImpactTree;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.navigation.NavigationService;
import com.jfrog.ide.idea.scan.ScannerBase;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.LocalComponentsTree;
import com.jfrog.ide.idea.utils.Descriptor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.TreeNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parent class of all inspections and annotations.
 * The inspections are the "Show in JFrog plugin" action.
 * The annotations are the "Top issue" and "Licenses" labels.
 *
 * @author yahavi
 */
public abstract class AbstractInspection extends LocalInspectionTool implements Annotator {

    private final String packageDescriptorName;
    // True if the code inspection was automatically triggered after an Xray scan using InspectionEngine.runInspectionOnFile(...).
    private boolean afterScan;

    AbstractInspection(Descriptor descriptor) {
        this.packageDescriptorName = descriptor.getFileName();
    }

    public void setAfterScan(boolean afterScan) {
        this.afterScan = afterScan;
    }

    /**
     * Get Psi element and decide whether to add "Show in JFrog plugin" option, and register a corresponding
     * navigation from item in tree to item in project-descriptor.
     *
     * @param problemsHolder - The "Show in JFrog plugin" option will be registered in this container.
     * @param element        - The Psi element in the package descriptor
     * @param isOnTheFly     - True if the inspection was triggered by opening a package descriptor file.
     *                       False if the inspection was triggered manually by clicking on "Code | Inspect Code".
     */
    void visitElement(ProblemsHolder problemsHolder, PsiElement element, boolean isOnTheFly) {
        if (!afterScan && !isOnTheFly) {
            // Code inspection was triggered manually by clicking on "Code | Inspect Code".
            return;
        }
        String componentName = createComponentName(element);
        if (StringUtils.isBlank(componentName)) {
            return; // Failed creating the component name
        }
        List<DependencyNode> dependencies = getDependencies(element, componentName);
        if (CollectionUtils.isEmpty(dependencies)) {
            return;
        }
        NavigationService navigationService = NavigationService.getInstance(element.getProject());
        for (DependencyNode dependency : dependencies) {
            if (isOnTheFly) {
                registerProblem(problemsHolder, dependency, element, componentName);
            }
            navigationService.addNavigation(dependency, element, componentName);
        }
    }

    /**
     * Get Psi element and decide whether to add licenses and top issue annotations.
     *
     * @param annotationHolder - The annotations will be registered in this container
     * @param element          - The Psi element in the package descriptor
     */
    void visitElement(AnnotationHolder annotationHolder, PsiElement element) {
        String componentName = createComponentName(element);
        if (componentName == null) {
            return; // Failed creating the component name
        }
        List<DependencyNode> dependencies = getDependencies(element, componentName);
        if (CollectionUtils.isNotEmpty(dependencies)) {
            AnnotationUtils.registerAnnotation(annotationHolder, dependencies.get(0), element, showAnnotationIcon(element));
        }
    }

    /**
     * Get the relevant scan manager according to the project type and path.
     *
     * @param project - The Project
     * @param path    - Path to project
     * @return ScanManager
     */
    abstract ScannerBase getScanner(Project project, String path);

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
        Enumeration<TreeNode> roots = ((SortableChildrenTreeNode) componentsTree.getModel().getRoot()).children();
        for (TreeNode root : Collections.list(roots)) {
            if (root instanceof DescriptorFileTreeNode fileNode) {
                if (fileNode.getFilePath().equals(element.getContainingFile().getVirtualFile().getPath())) {
                    fileDescriptors.add(fileNode);
                }
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


        ScannerBase scanner = getScanner(project, editorFile.getParent().getPath());
        if (scanner == null) {
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
    List<DependencyNode> getDependencies(PsiElement element, String componentName) {
        if (!isShowInspection(element)) {
            return null; // Inspection is not needed for this element
        }
        Set<DescriptorFileTreeNode> filesNodes = getFileDescriptors(element);
        if (filesNodes == null) {
            return null; // No files descriptors found for this element
        }
        return filesNodes.stream()
                .map(descriptorFile -> getMatchDependencies(descriptorFile, componentName))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Get the dependencies that match to the input componentName.
     *
     * @param file          - The Descriptor file node in the tree
     * @param componentName - Component name representing a dependency without version
     * @return the dependencies node that match to the input general info
     */
    private List<DependencyNode> getMatchDependencies(DescriptorFileTreeNode file, String componentName) {
        List<DependencyNode> dependencies = new ArrayList<>();
        for (DependencyNode dependency : file.getDependencies()) {
            if (isNodeMatch(dependency, componentName)) {
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    /**
     * Compare the component name from the Psi element and the dependency node from the Dependency tree.
     *
     * @param node          - the dependency node from the dependency tree
     * @param componentName - Component name representing a dependency without version
     * @return true if the node matches the component name
     */
    boolean isNodeMatch(DependencyNode node, String componentName) {
        String artifactID = node.getComponentIdWithoutPrefix();
        ImpactTree impactTree = node.getImpactTree();
        String versionPrefix = ":";
        return StringUtils.equals(extractArtifactIdWithoutVersion(artifactID), componentName) || impactTree.contains(componentName+versionPrefix);
    }

    abstract UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issues, String descriptorPath);

    void registerProblem(ProblemsHolder problemsHolder, DependencyNode dependency, PsiElement element, String componentName) {
        boolean isTransitive = dependency.isIndirect() || !StringUtils.contains(dependency.getTitle(), componentName);
        String dependencyDescription = getDependencyDescription(dependency.getTitle(), isTransitive);
        List<LocalQuickFix> quickFixes = new ArrayList<>();
        quickFixes.add(new ShowInDependencyTree(dependency, dependencyDescription));

        if (!isTransitive) {
            Multimap<String, String> fixVersionToCves = ArrayListMultimap.create();
            dependency.children().asIterator().forEachRemaining(issueNode -> {
                List<String> fixVersionStrings = ListUtils.emptyIfNull(((VulnerabilityNode) issueNode).getFixedVersions());
                for (String fixVersionString : fixVersionStrings) {
                    String fixVersion = convertFixVersionStringToMinFixVersion(fixVersionString);
                    fixVersionToCves.put(fixVersion, issueNode.toString());
                }
            });

            String descriptorPath = element.getContainingFile().getVirtualFile().getPath();
            fixVersionToCves.asMap().forEach((fixedVersion, issues) -> {
                UpgradeVersion upgradeVersion = getUpgradeVersion(dependency.getArtifactId(), fixedVersion, issues, descriptorPath);
                quickFixes.add(upgradeVersion);
            });
        }

        problemsHolder.registerProblem(
                element,
                "JFrog: " + dependencyDescription + " has security vulnerabilities",
                ProblemHighlightType.WARNING,
                quickFixes.toArray(LocalQuickFix[]::new)
        );
    }

    private String getDependencyDescription(String depComponent, boolean isTransitive) {
        String description = "dependency <" + depComponent + ">";
        if (isTransitive) {
            description = "transitive " + description;
        }
        return description;
    }

    protected static String convertFixVersionStringToMinFixVersion(String fixVersionString) {
        // Possible fix version string formats:
        // 1.0        >> 1.0
        // (,1.0]     >> N/A
        // (,1.0)     >> N/A
        // [1.0]      >> 1.0
        // (1.0,)     >> N/A
        // (1.0, 2.0) >> N/A
        // [1.0, 2.0] >> 1.0
        String fixVersion = fixVersionString.trim().split(",")[0];
        if (fixVersion.charAt(0) == '(') {
            // If first character is '(' then we can't tell what's the minimal fix version
            return "";
        }
        fixVersion = StringUtils.strip(fixVersion, "[");
        fixVersion = StringUtils.strip(fixVersion, "]");
        return fixVersion;
    }


    private String extractArtifactIdWithoutVersion(String artifact) {
        int versionIndex = artifact.lastIndexOf(':');

        if (versionIndex != -1) {
           return artifact.substring(0, versionIndex);
        } else {
            return artifact;
        }
    }
    }