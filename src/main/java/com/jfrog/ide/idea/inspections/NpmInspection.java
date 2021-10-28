package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.json.psi.JsonElementVisitor;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScanManagersFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.util.Set;

/**
 * @author yahavi
 */
@SuppressWarnings("InspectionDescriptionNotFoundInspection")
public class NpmInspection extends AbstractInspection {

    public NpmInspection() {
        super("package.json");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JsonElementVisitor() {
            @Override
            public void visitProperty(@NotNull JsonProperty element) {
                super.visitProperty(element);
                NpmInspection.this.visitElement(holder, element);
            }
        };
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof JsonProperty) {
            NpmInspection.this.visitElement(holder, element);
        }
    }

    @Override
    PsiElement[] getTargetElements(PsiElement element) {
        return new PsiElement[]{element};
    }

    @Override
    boolean isDependency(PsiElement element) {
        PsiElement parentElement = element.getParent().getParent();
        return parentElement != null && StringUtils.equalsAny(parentElement.getFirstChild().getText(), "\"dependencies\"", "\"devDependencies\"");
    }

    @Override
    ScanManager getScanManager(Project project, String path) {
        return ScanManagersFactory.getScanManagers(project).stream()
                .filter(manager -> StringUtils.equals(manager.getProjectPath(), path))
                .findAny()
                .orElse(null);
    }

    @Override
    Set<DependencyTree> getModules(PsiElement element, String componentName) {
        DependencyTree root = getRootDependencyTree(element);
        if (root == null) {
            return null;
        }
        return collectModules(root, element);
    }

    @Override
    String createComponentName(PsiElement element) {
        return StringUtils.unwrap(element.getFirstChild().getText(), "\"");
    }
}
