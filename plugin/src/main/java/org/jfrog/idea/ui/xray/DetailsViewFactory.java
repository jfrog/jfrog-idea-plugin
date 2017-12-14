package org.jfrog.idea.ui.xray;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static org.jfrog.idea.ui.utils.ComponentUtils.createDisabledTextLabel;
import static org.jfrog.idea.ui.utils.ComponentUtils.createJTextArea;

/**
 * Created by romang on 5/4/17.
 */
public class DetailsViewFactory extends JBPanel {

    public static void createIssuesDetailsView(JPanel panel, ScanTreeNode node) {
        JPanel gridPanel = createCommonGridPanel(panel, node);
        if (gridPanel == null) {
            return;
        }
        Issue topIssue = node.getTopIssue();
        addJtext(gridPanel, 5, "Top Issue Severity:", StringUtils.capitalize(topIssue.severity.toString()));
        addJtext(gridPanel, 6, "Top Issue Type:", StringUtils.capitalize(topIssue.issueType));
        addJtext(gridPanel, 7, "Issues Count:", String.valueOf(node.getIssueCount()));
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    public static void createLicenseDetailsView(JPanel panel, ScanTreeNode node) {
        JPanel gridPanel = createCommonGridPanel(panel, node);
        if (gridPanel == null) {
            return;
        }
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    private static JPanel createCommonGridPanel(JPanel panel, ScanTreeNode node) {
        if (node == null || node.getGeneralInfo() == null) {
            replaceAndUpdateUI(panel, createDisabledTextLabel("Component information is not available"),
                    BorderLayout.CENTER);
            return null;
        }
        JPanel gridPanel = new JBPanel(new GridBagLayout());
        gridPanel.setBackground(UIUtil.getTableBackground());
        addJtext(gridPanel, 0, "Group:", node.getGeneralInfo().getGroupId());
        addJtext(gridPanel, 1, "Artifact:", node.getGeneralInfo().getArtifactId());
        addJtext(gridPanel, 2, "Version:", node.getGeneralInfo().getVersion());
        addJtext(gridPanel, 3, "Type:", StringUtils.capitalize(node.getGeneralInfo().pkgType));
        addLicenses(gridPanel, node.getLicenses());
        return gridPanel;
    }

    private static void addLicenses(JPanel panel, Set<License> licenses) {
        if (licenses == null) {
            return;
        }
        JPanel licensesPanel = new JBPanel(new HorizontalLayout(1));
        licensesPanel.setBackground(UIUtil.getTableBackground());
        for (License license : licenses) {
            if (CollectionUtils.isEmpty(license.moreInfoUrl)) {
                licensesPanel.add(createJTextArea(createLicenseString(license), false));
                continue;
            }
            HyperlinkLabel hyperlinkLabel = new HyperlinkLabel(createLicenseString(license));
            hyperlinkLabel.setBackground(UIUtil.getTableBackground());
            hyperlinkLabel.setHyperlinkTarget(license.moreInfoUrl.get(0));
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

    private static String createLicenseString(License license) {
        if (license.fullName.equals("Unknown license")) {
            return license.name;
        }
        return license.fullName + " (" + license.name + ")";
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
        panel.add(createJTextArea(text, true), c);
    }
}
