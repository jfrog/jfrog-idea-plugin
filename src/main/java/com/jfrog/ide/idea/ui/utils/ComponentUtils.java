package com.jfrog.ide.idea.ui.utils;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.ui.ComponentsTree;
import com.jfrog.ide.idea.ui.configuration.XrayGlobalConfiguration;
import com.jfrog.ide.idea.ui.filters.IssueFilterMenu;
import com.jfrog.ide.idea.ui.filters.LicenseFilterMenu;
import com.jfrog.ide.idea.ui.filters.ScopeFilterMenu;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Created by romang on 5/7/17.
 */
public class ComponentUtils {

    public static final String UNSUPPORTED_TEXT = "Unsupported project type, currently only Maven, Gradle, Go and npm projects are supported.";
    public static final String SELECT_COMPONENT_TEXT = "Select component or issue for more details.";

    public static JTextArea createJTextArea(String text, boolean lineWrap) {
        JTextArea jTextArea = new JTextArea(text);
        jTextArea.setOpaque(true);
        jTextArea.setEditable(false);
        jTextArea.setLineWrap(lineWrap);
        jTextArea.setWrapStyleWord(true);
        jTextArea.setBackground(UIUtil.getTableBackground());
        return jTextArea;
    }

    public static JLabel createDisabledTextLabel(String text) {
        JLabel label = new JBLabel(text);
        label.setEnabled(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    public static JPanel createActionToolbar(String id, Project mainProject, ComponentsTree componentsTree) {
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
        defaultActionGroup.addAction(ActionManager.getInstance().getAction("Xray.Refresh"));
        defaultActionGroup.addAction(new CollapseAllAction(componentsTree));
        defaultActionGroup.addAction(new ExpandAllAction(componentsTree));

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(id, defaultActionGroup, true);
        JPanel toolbarPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbarPanel.add(actionToolbar.getComponent());

        // Add issues filter
        IssueFilterMenu issueFilterMenu = new IssueFilterMenu(mainProject);
        componentsTree.addFilterMenu(issueFilterMenu);
        toolbarPanel.add(issueFilterMenu.getFilterButton());

        // Add licenses filter
        LicenseFilterMenu licenseFilterMenu = new LicenseFilterMenu(mainProject);
        componentsTree.addFilterMenu(licenseFilterMenu);
        toolbarPanel.add(licenseFilterMenu.getFilterButton());

        // Add scopes filter
        ScopeFilterMenu scopeFilterMenu = new ScopeFilterMenu(mainProject);
        componentsTree.addFilterMenu(scopeFilterMenu);
        toolbarPanel.add(scopeFilterMenu.getFilterButton());

        return toolbarPanel;
    }

    public static JComponent createNoCredentialsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setHyperlinkText("To start using the JFrog Plugin, please ", " configure", " your JFrog Xray details.");
        link.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(null, XrayGlobalConfiguration.class));
        return createUnsupportedPanel(link);
    }

    private static JPanel createUnsupportedPanel(Component label) {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(label, c);
        panel.setBackground(UIUtil.getTableBackground());
        return panel;
    }

    public static String getPathSearchString(TreePath path) {
        DependenciesTree node = (DependenciesTree) path.getLastPathComponent();
        return node == null ? "" : node.toString();
    }
}