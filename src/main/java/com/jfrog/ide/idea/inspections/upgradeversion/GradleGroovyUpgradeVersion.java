package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

import java.util.Collection;

import static com.jfrog.ide.idea.inspections.GradleGroovyInspection.GRADLE_VERSION_KEY;
import static com.jfrog.ide.idea.inspections.GradleGroovyInspection.getLiteralValue;
import static com.jfrog.ide.idea.inspections.GradleInspection.stripVersion;


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
        PsiElement element = descriptor.getPsiElement();
        GroovyPsiElementFactory psiFactory = GroovyPsiElementFactory.getInstance(project);

        if (element instanceof GrNamedArgumentsOwner) {
            // group: 'com', name: 'guava', version: '1.1.1' >> group: 'com', name: 'guava', version: '2.2.2'
            GrNamedArgument versionArg = ((GrNamedArgumentsOwner) element).findNamedArgument(GRADLE_VERSION_KEY);
            if (versionArg != null && versionArg.getExpression() != null) {
                versionArg.getExpression().replace(psiFactory.createExpressionFromText(StringUtils.wrap(fixVersion, "'")));
                return;
            }
        }

        if (element instanceof GrLiteral) {
            // 'com:guava:1.1.1' >> 'com:guava:2.2.2'
            String componentString = getLiteralValue((GrLiteral) element);
            String fixedComponentString = String.join(":", stripVersion(componentString), fixVersion);
            element.replace(psiFactory.createExpressionFromText(StringUtils.wrap(fixedComponentString, "'")));
        }
    }
}