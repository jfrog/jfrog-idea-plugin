package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jfrog.ide.idea.inspections.upgradeversion.GradleGroovyUpgradeVersion;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;
import com.jfrog.ide.idea.utils.Descriptor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
public class GradleGroovyInspection extends GradleInspection {

    public static final String GRADLE_GROUP_KEY = "group";
    public static final String GRADLE_NAME_KEY = "name";
    public static final String GRADLE_VERSION_KEY = "version";

    public GradleGroovyInspection() {
        super(Descriptor.GRADLE_GROOVY);
    }

    /**
     * Get the string value of the groovy literal.
     *
     * @param literal - The groovy literal
     * @return the value of the literal
     */
    public static String getLiteralValue(GrLiteral literal) {
        String artifact = Objects.toString((literal).getValue(), "");
        int versionIndex = artifact.lastIndexOf(':');
        if (versionIndex == -1) {
            return artifact;
        }
        return artifact.substring(0, versionIndex);
    }

    public static boolean isNamedArgumentComponent(PsiElement element) {
        return (element instanceof GrNamedArgumentsOwner && ((GrNamedArgumentsOwner) element).getNamedArguments().length >= 3);
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new GroovyPsiElementVisitor(new GroovyElementVisitor() {
            @Override
            public void visitArgumentList(@NotNull GrArgumentList list) {
                super.visitArgumentList(list);
                List<GroovyPsiElement> elementsToVisit = parseComponentElements(list);
                for (GroovyPsiElement elementToVisit : elementsToVisit) {
                    GradleGroovyInspection.this.visitElement(holder, elementToVisit, isOnTheFly);
                }
            }
        });
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
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof GrArgumentList) {
            List<GroovyPsiElement> elementsToVisit = parseComponentElements((GrArgumentList) element);
            for (GroovyPsiElement elementToVisit : elementsToVisit) {
                GradleGroovyInspection.this.visitElement(holder, elementToVisit);
            }
        }
    }

    List<GroovyPsiElement> parseComponentElements(GrArgumentList element) {
        List<GroovyPsiElement> elementsToVisit = new ArrayList<>();
        if (isNamedArgumentComponent(element)) {
            // Example: implementation group: 'j', name: 'k', version: 'l'
            elementsToVisit.add(element);
        } else {
            // Example:
            // implementation([group: 'net.lingala.zip4j', name: 'zip4j', version: '2.3.0'],
            //                [group: 'org.codehaus.groovy', name: 'groovy-all', version: '3.0.5'])
            // OR
            // implementation("org.codehaus.groovy:groovy-all:3.0.5")
            // OR
            // implementation 'net.lingala.zip4j:zip4j:2.3.0',
            //                'org.codehaus.groovy:groovy-all:3.0.5'
            for (GroovyPsiElement subElement : element.getAllArguments()) {
                if (isNamedArgumentComponent(subElement) || (subElement instanceof GrLiteral)) {
                    elementsToVisit.add(subElement);
                } else if (subElement.getChildren().length > 0 && subElement.getChildren()[0] instanceof GrLiteral) {
                    elementsToVisit.add((GrLiteral) subElement.getChildren()[0]);
                }
            }
        }
        return elementsToVisit;
    }

    @Override
    String createComponentName(PsiElement element) {
        if (isNamedArgumentComponent(element)) {
            // implementation group: 'j', name: 'k', version: 'l'
            return String.join(":",
                    extractExpression(element, GRADLE_GROUP_KEY),
                    extractExpression(element, GRADLE_NAME_KEY));
        }
        if (element instanceof GrLiteral) {
            //  implementation 'g:h:i'
            return getLiteralValue((GrLiteral) element);
        }
        return "";
    }

    /**
     * Extract expression from groovy arguments.
     *
     * @param argumentList - The arguments list
     * @param name         - The name of the argument to extract
     * @return the value of the argument
     */
    private String extractExpression(PsiElement argumentList, String name) {
        GrNamedArgument argument = ((GrNamedArgumentsOwner) argumentList).findNamedArgument(name);
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

    @Override
    UpgradeVersion getUpgradeVersion(String componentName, String fixVersion, Collection<String> issue, String descriptorPath) {
        return new GradleGroovyUpgradeVersion(componentName, fixVersion, issue);
    }
}
