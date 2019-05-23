package com.jfrog.ide.idea.ui;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.utils.Utils;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.jfrog.build.extractor.scan.License;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * @author yahavi
 */
public class ComponentDetail extends JPanel {

    private int lastTextPosition = 0;

    public ComponentDetail(DependenciesTree node) {
        setLayout(new GridBagLayout());
        setBackground(UIUtil.getTableBackground());
        GeneralInfo generalInfo = node.getGeneralInfo();
        String pkgType = StringUtils.capitalize(generalInfo.getPkgType());
        if (StringUtils.equals(pkgType, "Npm")) {
            // Npm
            addText("Package:", generalInfo.getGroupId());
        } else {
            // Maven/Gradle
            addText("Group:", generalInfo.getGroupId());
            addText("Artifact:", generalInfo.getArtifactId());
        }
        addText("Version:", generalInfo.getVersion());
        addText("Type:", pkgType);
        addText("Path:", generalInfo.getPath());
        addLicenses(node.getLicenses());
    }

    private void addLicenses(Set<License> licenses) {
        if (licenses.isEmpty()) {
            return;
        }
        JPanel licensesPanel = new JBPanel(new HorizontalLayout(1));
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

        gridBagConstraints.gridy = lastTextPosition++;
        add(headerLabel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.9;
        add(licensesPanel, gridBagConstraints);
    }

    protected void addText(String header, String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        JLabel headerLabel = createHeaderLabel(header);
        GridBagConstraints gridBagConstraints = createGridBagConstraints();

        gridBagConstraints.gridy = lastTextPosition++;
        add(headerLabel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.9;
        add(ComponentUtils.createJTextArea(text, true), gridBagConstraints);
    }

    private JLabel createHeaderLabel(String title) {
        JLabel headerLabel = new JBLabel(title);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(UIUtil.getTableBackground());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        return headerLabel;
    }

    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.ipady = 3;
        return gridBagConstraints;
    }


    protected static void replaceAndUpdateUI(JPanel panel, JComponent component, Object constraint) {
        panel.removeAll();
        panel.add(component, constraint);
        panel.validate();
        panel.repaint();
    }

    protected static void createComponentInfoNotAvailablePanel(JPanel panel) {
        replaceAndUpdateUI(panel, ComponentUtils.createDisabledTextLabel("Component information is not available"), BorderLayout.CENTER);
    }

}
