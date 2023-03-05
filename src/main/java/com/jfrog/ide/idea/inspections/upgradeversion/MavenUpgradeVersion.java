package com.jfrog.ide.idea.inspections.upgradeversion;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.idea.maven.dom.MavenDomProjectProcessorUtils.findProperty;

/**
 * Adds the yellow bulb action - "Upgrade Version".
 *
 * @author michaels
 */
public class MavenUpgradeVersion extends UpgradeVersion {

    private static final Pattern POM_PROPERTY_REGEX = Pattern.compile("^\\$\\{(.*)}");

    public MavenUpgradeVersion(String componentName, String fixVersion, Collection<String> issue) {
        super(componentName, fixVersion, issue);
    }

    @Override
    public void upgradeComponentVersion(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final XmlTag[] versions = ((XmlTagImpl) descriptor.getPsiElement()).findSubTags("version");
        XmlTagValue versionTagValue = versions[0].getValue();
        Matcher propMatcher = POM_PROPERTY_REGEX.matcher(versionTagValue.getText());
        if (!propMatcher.find()) {
            // Simple version tag. (example: '<version>1.2.3</version>')
            versionTagValue.setText(fixVersion);
        } else {
            // Property version tag. (example: '<version>${my.ver}</version>')
            MavenDomProjectModel domModel = MavenDomUtil.getMavenDomProjectModel(project, descriptor.getPsiElement().getContainingFile().getVirtualFile());
            if (domModel != null) {
                XmlTag prop = findProperty(domModel.getProperties(), propMatcher.group(1));
                if (prop != null) {
                    prop.getValue().setText(fixVersion);
                }
            }
        }
    }
}