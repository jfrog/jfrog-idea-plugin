package org.jfrog.idea.ui.xray;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
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

    public static void createDetailsView(JBPanel panel, Issue issue) {
        if (issue == null) {
            return;
        }

        JBPanel gridPanel = new JBPanel(new GridBagLayout());
        gridPanel.setBackground(UIUtil.getTableBackground());
        addJlabel(gridPanel, "Issue Details");
        addJtext(gridPanel, 1, "Summary:", issue.summary);
        addJtext(gridPanel, 2, "Severity:", issue.sevirity);
        addJtext(gridPanel, 3, "Issue Type:", StringUtil.capitalize(issue.issueType));
        addJtext(gridPanel, 4, "Description:", issue.description);
        addJtext(gridPanel, 5, "Provider:", issue.provider);
        addJtext(gridPanel, 6, "Created:", issue.created);
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    public static void createDetailsView(JBPanel panel, ScanTreeNode node) {
        if (node == null || node.getGeneralInfo() == null) {
            replaceAndUpdateUI(panel, createDisabledTextLabel("Component information is not available"),
                    BorderLayout.CENTER);
            return;
        }

        JBPanel gridPanel = new JBPanel(new GridBagLayout());
        gridPanel.setBackground(UIUtil.getTableBackground());
        addJlabel(gridPanel, "Component Details");
        addJtext(gridPanel, 1, "Component ID:", node.getGeneralInfo().componentId);
        addJtext(gridPanel, 2, "Component Name:", node.getGeneralInfo().name);
        addJtext(gridPanel, 3, "Package type:", node.getGeneralInfo().pkgType);
        addLicenses(gridPanel, 4, "Licenses:", node.getLicenses());
        replaceAndUpdateUI(panel, gridPanel, BorderLayout.NORTH);
    }

    private static void addLicenses(JBPanel panel, int place, String header, Set<License> licenses) {
        if (licenses == null) {
            return;
        }
        JBPanel licensesPanel = new JBPanel(new HorizontalLayout(1));
        licensesPanel.setBackground(UIUtil.getTableBackground());
        for (License license : licenses) {
            if (license.moreInfoUrl == null || license.moreInfoUrl.isEmpty()) {
                licensesPanel.add(createJTextArea(license.fullName, false));
                continue;
            }

            HyperlinkLabel hyperlinkLabel = new HyperlinkLabel(license.fullName);
            hyperlinkLabel.setBackground(UIUtil.getTableBackground());
            hyperlinkLabel.setHyperlinkTarget(license.moreInfoUrl.get(0));
            licensesPanel.add(hyperlinkLabel);
        }

        JBLabel headerLabel = new JBLabel(header);
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
        panel.add(licensesPanel, c);
    }

    private static void replaceAndUpdateUI(JBPanel panel, JComponent component, Object constraint) {
        panel.removeAll();
        panel.add(component, constraint);
        panel.validate();
        panel.repaint();
    }

    private static void addJtext(JBPanel panel, int place, String header, String text) {
        JBLabel headerLabel = new JBLabel(header);
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

    private static void addJlabel(JBPanel gridPanel, String text) {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 20;
        c.ipady = 3;
        c.gridwidth = 2;
        gridPanel.add(createDisabledTextLabel(text), c);
    }
}
