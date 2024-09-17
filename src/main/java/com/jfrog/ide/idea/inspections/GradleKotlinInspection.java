package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jfrog.ide.idea.inspections.upgradeversion.GradleKotlinUpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.utils.Descriptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.List;

/**
 * Each dependency in Gradle-Kotlin is a function call with at least one argument.
 * Examples:
 * compile("a:b:c")
 * testCompile("d", "e", "f")
 * implementation(project(":project")) // Subproject dependencies start with ":"
 *
 * @author yahavi
 */
public class GradleKotlinInspection extends GradleInspection {

    public GradleKotlinInspection() {
        super(Descriptor.GRADLE_KOTLIN);
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new KtVisitorVoid() {
            @Override
            public void visitValueArgumentList(@NotNull KtValueArgumentList list) {
                // Verify that the visited file is a build.gradle.kts file
                if (((KtFile) list.getContainingFile()).isScript()) {
                    GradleKotlinInspection.this.visitElement(holder, list, isOnTheFly);
                }
            }
        };
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof KtValueArgumentList && ((KtFile) element.getContainingFile()).isScript()) {
            GradleKotlinInspection.this.visitElement(holder, element);
        }
    }

    @Override
    boolean isDependency(PsiElement element) {
        List<KtValueArgument> argumentList = ((KtValueArgumentList) element).getArguments();
        if (argumentList.isEmpty() || !(argumentList.get(0).getArgumentExpression() instanceof KtStringTemplateExpression)) {
            return false;
        }
        // Make sure the element is under "dependencies" scope
        for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
            if (!(parent instanceof KtCallExpression)) {
                continue;
            }
            KtExpression expression = ((KtCallExpression) parent).getCalleeExpression();
            if (expression != null && "dependencies".equals(expression.getText())) {
                return true;
            }
        }
        return false;
    }

    @Override
    String createComponentName(PsiElement element) {
        if (!(element instanceof KtValueArgumentList)) {
            return "";
        }
        List<KtValueArgument> argumentList = ((KtValueArgumentList) element).getArguments();
        if (argumentList.size() == 1) {
            // "commons-collections:commons-collections:3.2.2"
            String artifactId = extractArgument(argumentList.get(0));
            return StringUtils.substringBeforeLast(artifactId, ":");
        }
        if (argumentList.size() >= 3) {
            // "commons-collections", "commons-collections"
            return String.join(":",
                    extractArgument(argumentList.get(0)),
                    extractArgument(argumentList.get(1))
                    );
        }
        return "";
    }

    /**
     * Extract argument text from Kotlin argument.
     *
     * @param ktValueArgument - The arguments list
     * @return the value of the argument
     */
    private String extractArgument(KtValueArgument ktValueArgument) {
        // Remove quotes
        String value = ktValueArgument.getText().replaceAll("\"", "");

        // Remove '@' suffix, for example commons-lang:commons-lang:2.4@jar
        return StringUtils.substringBefore(value, "@");
    }

    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue, String descriptorPath) {
        return new GradleKotlinUpgradeVersion(componentName, fixVersion, issue);
    }

}
