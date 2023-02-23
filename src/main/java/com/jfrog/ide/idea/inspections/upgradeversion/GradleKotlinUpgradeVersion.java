package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.KtValueArgumentList;

import java.util.List;

/**
 * Adds the yellow bulb action - ""Upgrade Version"".
 *
 * @author michaels
 */
public class GradleKotlinUpgradeVersion extends UpgradeVersion {

    public GradleKotlinUpgradeVersion(String componentName, String fixVersion, String issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        List<KtValueArgument> argumentList = ((KtValueArgumentList) descriptor.getPsiElement()).getArguments();
        if (argumentList.size() > 0) {
            KtExpression ktExpression = argumentList.get(0).getArgumentExpression();
            if (ktExpression instanceof KtStringTemplateExpression) {
                KtStringTemplateExpression ktStringExpression = (KtStringTemplateExpression) ktExpression;
                // "commons-collections:commons-collections:3.2.2" >> "commons-collections:commons-collections:{NEW_VERSION}"
                String newVersionString = StringUtils.substringBeforeLast(ktStringExpression.getText(), ":") + ":" + fixVersion + "\"";
                ktStringExpression.updateText(newVersionString);
            }
        }
    }
}