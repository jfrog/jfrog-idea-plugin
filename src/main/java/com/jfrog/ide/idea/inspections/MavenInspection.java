package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomManager;
import com.jfrog.ide.idea.inspections.upgradeversion.MavenUpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.scan.MavenScanner;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScannerBase;
import com.jfrog.ide.idea.utils.Descriptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomArtifactCoordinates;
import java.util.Collection;
/**
 * @author yahavi
 */
public class MavenInspection extends AbstractInspection {

    public static final String MAVEN_DEPENDENCY_MANAGEMENT = "dependencyManagement";
    public static final String MAVEN_DEPENDENCIES_TAG = "dependencies";
    public static final String MAVEN_PLUGINS_TAG = "plugins";
    public static final String MAVEN_DEPENDENCY_TAG = "dependency";
    public static final String MAVEN_PLUGIN_TAG = "plugin";
    public static final String MAVEN_ARTIFACT_ID_TAG = "artifactId";
    public static final String MAVEN_GROUP_ID_TAG = "groupId";

    public MavenInspection() {
        super(Descriptor.MAVEN);
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new XmlElementVisitor() {
            @Override
            public void visitXmlTag(@NotNull XmlTag element) {
                if (isDependencyOrPlugin(element)) {
                    MavenInspection.this.visitElement(holder, element, isOnTheFly);
                }
            }
        };
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof XmlTag && isDependencyOrPlugin((XmlTag) element)) {
            MavenInspection.this.visitElement(holder, element);
        }
    }

    boolean isDependencyOrPlugin(XmlTag xmlTag) {
        return StringUtils.equalsAny(xmlTag.getName(), MAVEN_DEPENDENCY_TAG, MAVEN_PLUGIN_TAG);
    }

    @Override
    boolean isDependency(PsiElement element) {
        PsiElement parentElement = element.getParent();
        if ((parentElement instanceof XmlTag) &&
                StringUtils.equalsAny(((XmlTag) parentElement).getName(), MAVEN_DEPENDENCIES_TAG, MAVEN_PLUGINS_TAG)) {
            return true;
        }
        PsiElement grandParentElement = parentElement.getParent();
        return (grandParentElement instanceof XmlTag &&
                StringUtils.equals(((XmlTag) grandParentElement).getName(), MAVEN_DEPENDENCY_MANAGEMENT));
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
        DomElement domElement = DomManager.getDomManager(element.getProject()).getDomElement((XmlTag) element);
        if (domElement instanceof MavenDomArtifactCoordinates) {
            return String.join(":", groupId.getValue().getText(), artifactId.getValue().getText());
        }
        return null;
    }

    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue, String descriptorPath) {
        return new MavenUpgradeVersion(componentName, fixVersion, issue);
    }

}

