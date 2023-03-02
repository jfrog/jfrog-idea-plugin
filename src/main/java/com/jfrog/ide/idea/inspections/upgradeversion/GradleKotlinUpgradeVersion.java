package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtStringTemplateExpression;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.KtValueArgumentList;

import java.util.Collection;
import java.util.List;

import static com.jfrog.ide.idea.inspections.GradleInspection.stripVersion;

/**
 * Adds the yellow bulb action - ""Upgrade Version"".
 *
 * @author michaels
 */
public class GradleKotlinUpgradeVersion extends UpgradeVersion {

    public GradleKotlinUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        List<KtValueArgument> argumentList = ((KtValueArgumentList) descriptor.getPsiElement()).getArguments();
        String updateText = "";
        KtExpression expressionToUpdate = null;

        if (argumentList.size() == 1) {
            // "commons-collections:commons-collections:3.2.2"
            expressionToUpdate = argumentList.get(0).getArgumentExpression();
            String stripQuotes = StringUtils.unwrap(expressionToUpdate.getText(), "\"");
            updateText = stripVersion(stripQuotes) + ":" + fixVersion;
        } else if (argumentList.size() >= 3) {
            // "commons-collections", "commons-collections", "3.2.2"
            expressionToUpdate = argumentList.get(2).getArgumentExpression();
            updateText = fixVersion;
        }

        if (expressionToUpdate instanceof KtStringTemplateExpression) {
            ((KtStringTemplateExpression) expressionToUpdate).updateText(StringUtils.wrap(updateText, "\""));
        }
    }
}