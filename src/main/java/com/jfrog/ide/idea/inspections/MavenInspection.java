package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlTag;
import com.jfrog.ide.idea.scan.MavenScanner;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScannerBase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author yahavi
 */
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
                MavenInspection.this.visitElement(holder, element, isOnTheFly);
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
                !StringUtils.equals(((XmlTag) grandParentElement).getName(), MAVEN_DEPENDENCY_MANAGEMENT);
    }

    @Override
    ScannerBase getScanner(Project project, String path) {
        return ScanManager.getScanners(project).stream()
                .filter(MavenScanner.class::isInstance)
                .findAny()
                .orElse(null);
    }

    @Override
    String createComponentName(PsiElement element) {
        XmlTag groupId = ((XmlTagImpl) element).findFirstSubTag(MAVEN_GROUP_ID_TAG);
        XmlTag artifactId = ((XmlTagImpl) element).findFirstSubTag(MAVEN_ARTIFACT_ID_TAG);
        if (groupId == null || artifactId == null) {
            return null;
        }
        return String.join(":", groupId.getValue().getText(), artifactId.getValue().getText());
    }

}
