package com.jfrog.ide.idea.ui.utils;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.ui.configuration.JFrogGlobalConfiguration;
import org.jfrog.build.extractor.scan.DependencyTree;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * Created by romang on 5/7/17.
 */
public class ComponentUtils {

    public static final String UNSUPPORTED_TEXT = "Unsupported project type, currently only Maven, Gradle, Go, npm, and PyPI projects are supported.";
    public static final String SELECT_COMPONENT_TEXT = "Select component or issue for more details.";

    public static JTextArea createJTextArea(String text, boolean lineWrap) {
        JTextArea jTextArea = new JTextArea(text);
        jTextArea.setOpaque(true);
        jTextArea.setEditable(false);
        jTextArea.setLineWrap(lineWrap);
        jTextArea.setWrapStyleWord(true);
        jTextArea.setBackground(UIUtil.getTableBackground());
        jTextArea.setMargin(new JBInsets(2, 2, 2, 2));
        return jTextArea;
    }

    public static JLabel createDisabledTextLabel(String text) {
        JLabel label = new JBLabel(text);
        label.setEnabled(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static JComponent createNoCredentialsView() {
        JPanel noCredentialsPanel = new JBPanel<>();
        noCredentialsPanel.setLayout(new BoxLayout(noCredentialsPanel, BoxLayout.PAGE_AXIS));

        // "Thank you for installing the JFrog plugin"
        HyperlinkLabel thanksLabel = new HyperlinkLabel();
        thanksLabel.setText("Thank you for installing the JFrog plugin.");
        addCenteredHyperlinkLabel(noCredentialsPanel, thanksLabel);

        // "If you already have a JFrog environment, please configure its connection details."
        HyperlinkLabel configLink = new HyperlinkLabel();
        configLink.setTextWithHyperlink("If you already have a JFrog environment, please<hyperlink>configure</hyperlink> its connection details.");
        configLink.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(null, JFrogGlobalConfiguration.class));
        addCenteredHyperlinkLabel(noCredentialsPanel, configLink);

        // "Don't have a JFrog environment? Get one for FREE"
        HyperlinkLabel getFreeLink = new HyperlinkLabel();
        getFreeLink.setTextWithHyperlink("Don't have a JFrog environment?<hyperlink>Get one for FREE</hyperlink>");
        getFreeLink.addHyperlinkListener(e -> BrowserUtil.browse("https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin#JFrogIntelliJIDEAPlugin-SetUpaFREEJFrogEnvironmentintheCloud"));
        addCenteredHyperlinkLabel(noCredentialsPanel, getFreeLink);

        return createUnsupportedPanel(noCredentialsPanel);
    }

    /**
     * Add centered HyperlinkLabel to the input panel.
     *
     * @param panel          - The input panel
     * @param hyperlinkLabel - The hyperlink label
     */
    private static void addCenteredHyperlinkLabel(JPanel panel, HyperlinkLabel hyperlinkLabel) {
        hyperlinkLabel.setMaximumSize(hyperlinkLabel.getPreferredSize());
        hyperlinkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hyperlinkLabel);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static JComponent createNoBuildsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setTextWithHyperlink("No builds detected. To start viewing your builds please follow <hyperlink>this</hyperlink> guide.");
        link.addHyperlinkListener(e -> BrowserUtil.browse("https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin#JFrogIntelliJIDEAPlugin-TheCIView"));
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
        DependencyTree node = (DependencyTree) path.getLastPathComponent();
        return node == null ? "" : node.toString();
    }

    public static void replaceAndUpdateUI(JPanel panel, JComponent component, Object constraint) {
        panel.removeAll();
        panel.add(component, constraint);
        panel.validate();
        panel.repaint();
    }
}