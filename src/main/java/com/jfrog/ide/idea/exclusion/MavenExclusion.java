package com.jfrog.ide.idea.exclusion;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jfrog.ide.idea.inspections.MavenInspection;
import com.jfrog.ide.idea.navigation.NavigationTarget;
import org.jfrog.build.extractor.scan.DependenciesTree;

/**
 * Created by Bar Belity on 28/05/2020.
 */
public class MavenExclusion implements Excludable {

    public static final String MAVEN_EXCLUSIONS_TAG = "exclusions";
    public static final String MAVEN_EXCLUSION_TAG = "exclusion";
    private DependenciesTree nodeToExclude;
    private NavigationTarget navigationTarget;

    public MavenExclusion(DependenciesTree nodeToExclude, NavigationTarget navigationTarget) {
        this.nodeToExclude = nodeToExclude;
        this.navigationTarget = navigationTarget;
    }

    /**
     * Walk up the dependencies-tree to validate that the whole path is of 'Maven' nodes.
     * This is required as dependency nodes in 'Gradle' projects are also of type 'Maven'.
     * @param nodeToExclude - The node in tree to exclude.
     * @param affectedNode - Direct dependency's node in tree which will be affected by the exclusion.
     * @return true if nodeToExclude is a valid Maven node which can be excluded.
     */
    public static boolean isExcludable(DependenciesTree nodeToExclude, DependenciesTree affectedNode) {
        DependenciesTree currNode = nodeToExclude;
        // If currNode.getGeneralInfo() returns null, meaning there are several projects in the tree and reached the
        // top level node.
        while (currNode != null && currNode.getGeneralInfo() != null) {
            if (!isMavenPackageType(currNode)) {
                return false;
            }
            currNode = (DependenciesTree) currNode.getParent();
        }
        return !nodeToExclude.equals(affectedNode);
    }

    public static boolean isMavenPackageType(DependenciesTree node) {
        return node.getGeneralInfo() != null && "maven".equals(node.getGeneralInfo().getPkgType());
    }

    @Override
    public void exclude(Project project) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(navigationTarget.getElement().getContainingFile().getVirtualFile());
        if (!(psiFile instanceof XmlFile)) {
            return;
        }
        XmlFile file = (XmlFile) psiFile;

        WriteCommandAction.writeCommandAction(project, file).run(() -> {
            String groupId = nodeToExclude.getGeneralInfo().getGroupId();
            String artifactId = nodeToExclude.getGeneralInfo().getArtifactId();
            if (!(navigationTarget.getElement() instanceof XmlTag)) {
                return;
            }
            XmlTag xmlElement = (XmlTag) navigationTarget.getElement();
            navigateToElement(xmlElement);
            XmlTag exclusionsTag = xmlElement.findFirstSubTag(MAVEN_EXCLUSIONS_TAG);
            if (exclusionsTag == null) {
                exclusionsTag = xmlElement.createChildTag(MAVEN_EXCLUSIONS_TAG, "", "", false);
                createAndAddExclusionTags(exclusionsTag, groupId, artifactId);
                xmlElement.addSubTag(exclusionsTag, false);
                return;
            }

            XmlTag[] allExclusions = exclusionsTag.findSubTags(MAVEN_EXCLUSION_TAG);
            if (exclusionExists(allExclusions, groupId, artifactId)) {
                // Don't create exclusion tag.
                return;
            }
            createAndAddExclusionTags(exclusionsTag, groupId, artifactId);
        });
    }

    private boolean exclusionExists(XmlTag[] allExclusions, String groupId, String artifactId) {
        for (XmlTag exclusionTag : allExclusions) {
            XmlTag groupIdTag = exclusionTag.findFirstSubTag(MavenInspection.MAVEN_GROUP_ID_TAG);
            if (groupIdTag == null || !groupId.equals(groupIdTag.getValue().getText())) {
                continue;
            }
            XmlTag artifactIdTag = exclusionTag.findFirstSubTag(MavenInspection.MAVEN_ARTIFACT_ID_TAG);
            if (artifactIdTag != null && artifactId.equals(artifactIdTag.getValue().getText())) {
                return true;
            }
        }
        return false;
    }

    private void createAndAddExclusionTags(XmlTag exclusionsTag, String groupId, String artifactId) {
        XmlTag exclusionTag = exclusionsTag.createChildTag(MAVEN_EXCLUSION_TAG, "", "", false);
        XmlTag groupIdTag = exclusionTag.createChildTag(MavenInspection.MAVEN_GROUP_ID_TAG, "", groupId, false);
        XmlTag artifactIdTag = exclusionTag.createChildTag(MavenInspection.MAVEN_ARTIFACT_ID_TAG, "", artifactId, false);
        exclusionTag.addSubTag(groupIdTag, true);
        exclusionTag.addSubTag(artifactIdTag, false);
        exclusionsTag.addSubTag(exclusionTag, false);
    }

    private void navigateToElement(XmlTag xmlElement) {
        PsiElement navigationTarget = xmlElement.getNavigationElement();
        if (!(navigationTarget instanceof Navigatable)) {
            return;
        }
        Navigatable navigatable = (Navigatable) navigationTarget;
        if (navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }
}
