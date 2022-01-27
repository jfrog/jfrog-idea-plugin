package com.jfrog.ide.idea.ui;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;
import org.jfrog.build.extractor.scan.Scope;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yahavi
 */
public class ComponentDetails extends MoreInfoPanel {
    public ComponentDetails(DependencyTree node) {
        super();
        GeneralInfo generalInfo = node.getGeneralInfo();
        String pkgType = StringUtils.capitalize(generalInfo.getPkgType());
        if (StringUtils.equalsAny(pkgType, "Npm", "Go")) {
            addText("Package", generalInfo.getGroupId());
        } else {
            // Maven/Gradle
            addText("Group", generalInfo.getGroupId());
            addText("Artifact", generalInfo.getArtifactId());
        }
        addText("Version", generalInfo.getVersion());
        addText("Type", pkgType);
        addScopes(node.getScopes());
        addText("Path", generalInfo.getPath());
        addLicenses(node.getLicenses());
    }

    private void addScopes(Set<Scope> scopes) {
        if (scopes.size() == 1 && scopes.contains(new Scope())) {
            // Don't show 'None' scope if this is the only one scope
            return;
        }
        addText("Scopes", scopes.stream().map(Scope::toString).collect(Collectors.joining(",")));
    }

    private void addLicenses(Set<License> licenses) {
        if (licenses.isEmpty()) {
            return;
        }
        JPanel licensesPanel = new JBPanel<>(new HorizontalLayout(1));
        licensesPanel.setBackground(UIUtil.getTableBackground());
        for (License license : licenses) {
            if (CollectionUtils.isEmpty(license.getMoreInfoUrl())) {
                licensesPanel.add(ComponentUtils.createJTextArea(Utils.createLicenseString(license), false));
                continue;
            }
            HyperlinkLabel hyperlinkLabel = new HyperlinkLabel(Utils.createLicenseString(license));
            hyperlinkLabel.setBackground(UIUtil.getTableBackground());
            hyperlinkLabel.setHyperlinkTarget(license.getMoreInfoUrl().get(0));
            licensesPanel.add(hyperlinkLabel);
        }
        JLabel headerLabel = createHeaderLabel("Licenses:");
        GridBagConstraints gridBagConstraints = createGridBagConstraints();
        add(headerLabel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.9;
        add(licensesPanel, gridBagConstraints);
    }
}
