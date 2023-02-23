package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

import java.util.Collection;

/**
 * Adds the yellow bulb action - ""Upgrade Version"".
 *
 * @author michaels
 */
public class GradleGroovyUpgradeVersion extends UpgradeVersion {

    public GradleGroovyUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final GrNamedArgument versionArg = ((GrNamedArgumentsOwner) descriptor.getPsiElement()).findNamedArgument("version");
        if (versionArg.getExpression() != null) {
            versionArg.getExpression().replace(GroovyPsiElementFactory.getInstance(project).createExpressionFromText("'" + fixVersion + "'"));
        }
    }
}