package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jfrog.ide.idea.inspections.upgradeversion.GradleGroovyUpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

import java.util.Collection;
import java.util.Objects;

/**
 * @author yahavi
 */
public class GradleGroovyInspection extends GradleInspection {

    public static final String GRADLE_GROUP_KEY = "group";
    public static final String GRADLE_NAME_KEY = "name";

    public GradleGroovyInspection() {
        super("build.gradle");
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new GroovyPsiElementVisitor(new GroovyElementVisitor() {
            @Override
            public void visitArgumentList(@NotNull GrArgumentList list) {
                GradleGroovyInspection.this.visitElement(holder, list, isOnTheFly);
            }

        });
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof GrLiteral) {
            GradleGroovyInspection.this.visitElement(holder, element);
        }
    }

    @Override
    boolean isDependency(PsiElement element) {
        PsiElement parent = element.getParent();
        for (int i = 0; i < 6; i++, parent = parent.getParent()) {
            if (StringUtils.startsWith(parent.getText(), "dependencies")) {
                return true;
            }
        }
        return false;
    }

    @Override
    String createComponentName(PsiElement element) {
        if (element instanceof GrNamedArgumentsOwner) {
            GrNamedArgumentsOwner argOwner = (GrNamedArgumentsOwner) element;
            String group = extractExpression(argOwner, GRADLE_GROUP_KEY);
            String name = extractExpression(argOwner, GRADLE_NAME_KEY);
            if (StringUtils.isNotEmpty(group) && StringUtils.isNotEmpty(name)) {
                return String.join(":", group, name);
            }
        }
        String componentId = getLiteralValue((GrLiteral) element);
        return super.createComponentName(componentId);
    }

    /**
     * Extract expression from groovy arguments.
     *
     * @param argumentList - The arguments list
     * @param name         - The name of the argument to extract
     * @return the value of the argument
     */
    private String extractExpression(GrNamedArgumentsOwner argumentList, String name) {
        GrNamedArgument argument = argumentList.findNamedArgument(name);
        if (argument == null) {
            return "";
        }
        GrExpression grExpression = argument.getExpression();
        if (grExpression == null) {
            return "";
        }
        if (!(grExpression instanceof GrLiteral)) {
            return "";
        }
        return getLiteralValue((GrLiteral) grExpression);
    }

    /**
     * Get the string value of the groovy literal.
     *
     * @param literal - The groovy literal
     * @return the value of the literal
     */
    private String getLiteralValue(GrLiteral literal) {
        return Objects.toString((literal).getValue(), "");
    }

    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        return new GradleGroovyUpgradeVersion(componentName, fixVersion, issue);
    }
}
