package com.jfrog.ide.idea.inspections;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlTag;
import com.jfrog.ide.idea.scan.MavenScanManager;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.util.List;
import java.util.Set;

/**
 * @author yahavi
 */
@SuppressWarnings("InspectionDescriptionNotFoundInspection")
public class MavenInspection extends AbstractInspection {

    public static final String MAVEN_DEPENDENCY_MANAGEMENT = "dependencyManagement";
    public static final String MAVEN_DEPENDENCIES_TAG = "dependencies";
    public static final String MAVEN_ARTIFACT_ID_TAG = "artifactId";
    public static final String MAVEN_GROUP_ID_TAG = "groupId";
    public static final String MAVEN_VERSION_TAG = "version";

    public MavenInspection() {
        super("pom.xml");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag element) {
                super.visitElement(element);
                MavenInspection.this.visitElement(holder, element);
            }
        };
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof XmlTag) {
            MavenInspection.this.visitElement(holder, element);
        }
    }

    @Override
    PsiElement[] getTargetElements(PsiElement element) {
        XmlTag xmlTag = (XmlTag) element;
        PsiElement groupId = xmlTag.findFirstSubTag(MAVEN_GROUP_ID_TAG);
        PsiElement artifactId = xmlTag.findFirstSubTag(MAVEN_ARTIFACT_ID_TAG);
        PsiElement version = xmlTag.findFirstSubTag(MAVEN_VERSION_TAG);
        return new PsiElement[]{groupId, artifactId, version};
    }

    @Override
    boolean isDependency(PsiElement element) {
        PsiElement parentElement = element.getParent();
        if (!(parentElement instanceof XmlTag) ||
                !StringUtils.equals(((XmlTag) parentElement).getName(), MAVEN_DEPENDENCIES_TAG)) {
            return false;
        }
        PsiElement grandParentElement = parentElement.getParent();
        return !(grandParentElement instanceof XmlTag) ||
                !StringUtils.equals(((XmlTag) parentElement).getName(), MAVEN_DEPENDENCY_MANAGEMENT);
    }

    @Override
    ScanManager getScanManager(Project project, String path) {
        return ScanManagersFactory.getScanManagers(project).stream()
                .filter(MavenScanManager.class::isInstance)
                .findAny()
                .orElse(null);
    }

    @Override
    GeneralInfo createGeneralInfo(PsiElement element) {
        XmlTag groupId = ((XmlTagImpl) element).findFirstSubTag(MAVEN_GROUP_ID_TAG);
        XmlTag artifactId = ((XmlTagImpl) element).findFirstSubTag(MAVEN_ARTIFACT_ID_TAG);
        if (groupId == null || artifactId == null) {
            return null;
        }
        return new GeneralInfo().groupId(groupId.getValue().getText()).artifactId(artifactId.getValue().getText());
    }

    @Override
    Set<DependenciesTree> getModules(PsiElement element, GeneralInfo generalInfo) {
        Project project = element.getProject();
        DependenciesTree root = getRootDependenciesTree(element);
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
        if (root == null || mavenProjectsManager == null) {
            return null;
        }

        List<MavenProject> mavenModules = mavenProjectsManager.getProjects();
        // Single project, single module
        if (mavenModules.size() <= 1 && root.getGeneralInfo() != null) {
            return Sets.newHashSet(root);
        }

        // Multi project
        root = getProjectNode(root, project);

        // Multi module
        if (isModule(mavenModules, generalInfo)) {
            return Sets.newHashSet(root);
        }

        // Search for the relevant module
        return collectModules(element, mavenProjectsManager, root);
    }

    /**
     * Return true iff the dependency stated in the General info is a module in the project.
     *
     * @param mavenModules - The maven modules
     * @param generalInfo  - General info of the dependency
     * @return true iff the dependency stated in the General info is a module in the project
     */
    private boolean isModule(List<MavenProject> mavenModules, GeneralInfo generalInfo) {
        return mavenModules.stream()
                .map(MavenProject::getMavenId)
                .map(mavenId -> new GeneralInfo().artifactId(mavenId.getArtifactId()).groupId(mavenId.getGroupId()))
                .anyMatch(moduleGeneralInfo -> compareGeneralInfos(generalInfo, moduleGeneralInfo));
    }

    /**
     * Collect all modules containing the dependency stated in the general info.
     *
     * @param element              - The element containing the dependency
     * @param mavenProjectsManager - Maven project manager
     * @return list of all nodes of the Maven modules
     */
    private Set<DependenciesTree> collectModules(PsiElement element, MavenProjectsManager mavenProjectsManager, DependenciesTree node) {
        MavenProject mavenProject = mavenProjectsManager.findProject(element.getContainingFile().getVirtualFile());
        if (mavenProject == null) {
            return null;
        }
        node = node.getChildren().stream()
                .filter(child -> StringUtils.equals(child.getGeneralInfo().getArtifactId(), mavenProject.getMavenId().getArtifactId()))
                .filter(child -> StringUtils.equals(child.getGeneralInfo().getGroupId(), mavenProject.getMavenId().getGroupId()))
                .findAny()
                .orElse(node);
        return Sets.newHashSet(node);
    }
}
