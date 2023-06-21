package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.json.psi.JsonElementVisitor;
import com.intellij.json.psi.JsonProperty;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jfrog.ide.idea.inspections.upgradeversion.NpmUpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.scan.NpmScanner;
import com.jfrog.ide.idea.scan.ScanManager;
import com.jfrog.ide.idea.scan.ScannerBase;
import com.jfrog.ide.idea.utils.Descriptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author yahavi
 */
public class NpmInspection extends AbstractInspection {

    public NpmInspection() {
        super(Descriptor.NPM);
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JsonElementVisitor() {
            @Override
            public void visitProperty(@NotNull JsonProperty element) {
                super.visitProperty(element);
                NpmInspection.this.visitElement(holder, element, isOnTheFly);
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
    boolean isDependency(PsiElement element) {
        PsiElement parentElement = element.getParent().getParent();
        return parentElement != null && StringUtils.equalsAny(parentElement.getFirstChild().getText(), "\"dependencies\"", "\"devDependencies\"");
    }

    @Override
    ScannerBase getScanner(Project project, String path) {
        return ScanManager.getScanners(project).stream()
                .filter(manager -> StringUtils.equals(manager.getProjectPath(), path))
                .filter(this::isMatchingScanner)
                .findAny()
                .orElse(null);
    }

    boolean isMatchingScanner(ScannerBase scanner) {
        return scanner instanceof NpmScanner;
    }

    @Override
    String createComponentName(PsiElement element) {
        return StringUtils.unwrap(element.getFirstChild().getText(), "\"");
    }

    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        return new NpmUpgradeVersion(componentName, fixVersion, issue);
    }
}
