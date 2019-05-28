package com.jfrog.ide.idea.ui.utils;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.ui.configuration.XrayGlobalConfiguration;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;

/**
 * Created by romang on 5/7/17.
 */
public class ComponentUtils {

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

    public static JPanel createIssueCountLabel(int issueCount, int rowHeight) {
        JPanel issueCountPanel = new JBPanel(new BorderLayout()).withBackground(UIUtil.getTableBackground());
        JLabel issueCountLabel = new JBLabel();
        issueCountPanel.add(issueCountLabel, BorderLayout.EAST);
        setIssueCountPanel(issueCount, issueCountPanel);

        issueCountLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        issueCountLabel.setMinimumSize(new JBDimension(issueCountLabel.getMinimumSize().width, rowHeight));
        issueCountLabel.setMaximumSize(new JBDimension(issueCountLabel.getMaximumSize().width, rowHeight));

        issueCountPanel.setMinimumSize(new JBDimension(issueCountPanel.getMinimumSize().width, rowHeight));
        issueCountPanel.setMaximumSize(new JBDimension(issueCountPanel.getMaximumSize().width, rowHeight));
        return issueCountPanel;
    }

    public static void setIssueCountPanel(int issueCount, JPanel issueCountPanel) {
        JLabel issueCountLabel = (JLabel) issueCountPanel.getComponent(0);
        if (issueCount != 0) {
            issueCountLabel.setText(" (" + issueCount + ") ");
        } else {
            issueCountLabel.setText("");
        }
    }

    public static TreePath getTreePath(TreeNode treeNode) {
        List<Object> nodes = Lists.newArrayList(treeNode);
        while ((treeNode = treeNode.getParent()) != null) {
            nodes.add(0, treeNode);
        }
        return new TreePath(nodes.toArray());
    }

    public static ActionToolbar createActionToolbar(Tree componentsTree) {
        DefaultActionGroup defaultActionGroup = new DefaultActionGroup();
        defaultActionGroup.addAction(ActionManager.getInstance().getAction("Xray.Refresh"));
        defaultActionGroup.addAction(new CollapseAllAction(componentsTree));
        defaultActionGroup.addAction(new ExpandAllAction(componentsTree));
        return ActionManager.getInstance().createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, defaultActionGroup, true);
    }

    public static JComponent createNoCredentialsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setHyperlinkText("To start using the JFrog Plugin, please ", "configure", " your JFrog Xray details.");
        link.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(null, XrayGlobalConfiguration.class));
        return createUnsupportedPanel(link);
    }

    public static JPanel createUnsupportedView() {
        JLabel label = new JBLabel("Unsupported project type, currently only Maven, Gradle and npm projects are supported.");
        return createUnsupportedPanel(label);
    }

    private static JPanel createUnsupportedPanel(Component label) {
        JBPanel panel = new JBPanel(new GridBagLayout());
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