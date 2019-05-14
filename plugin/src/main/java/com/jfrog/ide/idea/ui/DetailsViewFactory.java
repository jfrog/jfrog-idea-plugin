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
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.License;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Created by romang on 5/4/17.
 */
public class DetailsViewFactory extends JBPanel {

    public static void createIssuesDetailsView(JPanel panel, DependenciesTree node) {
        JPanel gridPanel = createCommonGridPanel(panel, node);
        if (gridPanel == null) {
            return;
        }
        Issue topIssue = node.getTopIssue();
        addJtext(gridPanel, 5, "Top Issue Severity:", StringUtils.capitalize(topIssue.getSeverity().toString()));
        addJtext(gridPanel, 6, "Top Issue Type:", StringUtils.capitalize(topIssue.getIssueType()));
        addJtext(gridPanel, 7, "Issues Count:", String.valueOf(node.getIssueCount()));
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    public static void createLicenseDetailsView(JPanel panel, DependenciesTree node) {
        JPanel gridPanel = createCommonGridPanel(panel, node);
        if (gridPanel == null) {
            return;
        }
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    private static JPanel createCommonGridPanel(JPanel panel, DependenciesTree node) {
        if (node == null || node.getGeneralInfo() == null) {
            replaceAndUpdateUI(panel, ComponentUtils.createDisabledTextLabel("Component information is not available"),
                    BorderLayout.CENTER);
            return null;
        }
        JPanel gridPanel = new JBPanel(new GridBagLayout());
        gridPanel.setBackground(UIUtil.getTableBackground());
        GeneralInfo generalInfo = node.getGeneralInfo();
        String pkgType = StringUtils.capitalize(generalInfo.getPkgType());
        if (StringUtils.isBlank(pkgType)) {
            // No package type
            addJtext(gridPanel, 0, "Group:", generalInfo.getGroupId());
            addJtext(gridPanel, 1, "Artifact:", generalInfo.getArtifactId());
            addJtext(gridPanel, 2, "Version:", generalInfo.getVersion());
        } else if (pkgType.equals("Npm")) {
            // Npm
            addJtext(gridPanel, 0, "Package:", generalInfo.getGroupId());
            addJtext(gridPanel, 1, "Version:", generalInfo.getVersion());
            addJtext(gridPanel, 2, "Type:", pkgType);
            if (StringUtils.isNotBlank(generalInfo.getPath())) {
                addJtext(gridPanel, 3, "Path:", generalInfo.getPath());
            }
        } else {
            // Maven/Gradle
            addJtext(gridPanel, 0, "Group:", generalInfo.getGroupId());
            addJtext(gridPanel, 1, "Artifact:", generalInfo.getArtifactId());
            addJtext(gridPanel, 2, "Version:", generalInfo.getVersion());
            addJtext(gridPanel, 3, "Type:", pkgType);
        }
        addLicenses(gridPanel, node.getLicenses());
        return gridPanel;
    }

    private static void addLicenses(JPanel panel, Set<License> licenses) {
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

        JBLabel headerLabel = new JBLabel("Licenses:");
        headerLabel.setBackground(UIUtil.getTableBackground());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.ipadx = 20;
        c.ipady = 3;

        c.gridy = 4;
        panel.add(headerLabel, c);

        c.gridx = 1;
        c.weightx = 0.9;
        panel.add(licensesPanel, c);
    }

    private static void replaceAndUpdateUI(JPanel panel, JComponent component, Object constraint) {
        panel.removeAll();
        panel.add(component, constraint);
        panel.validate();
        panel.repaint();
    }

    private static void addJtext(JPanel panel, int place, String header, String text) {
        JLabel headerLabel = new JBLabel(header);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(UIUtil.getTableBackground());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.ipadx = 20;
        c.ipady = 3;

        c.gridy = place;
        panel.add(headerLabel, c);

        c.gridx = 1;
        c.weightx = 0.9;
        panel.add(ComponentUtils.createJTextArea(text, true), c);
    }
}
