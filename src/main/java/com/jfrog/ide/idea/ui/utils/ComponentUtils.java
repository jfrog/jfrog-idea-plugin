package com.jfrog.ide.idea.ui.utils;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
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

    public static JComponent createNoCredentialsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setHyperlinkText("To start using the JFrog Plugin, please ", " configure", " your JFrog platform details.");
        link.addHyperlinkListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(null, JFrogGlobalConfiguration.class));
        return createUnsupportedPanel(link);
    }

    public static JComponent createNoBuildsView() {
        HyperlinkLabel link = new HyperlinkLabel();
        link.setHyperlinkText("No builds detected. To start viewing your builds please follow ", " this", " guide.");
        link.addHyperlinkListener(e -> BrowserUtil.browse("https://www.jfrog.com/confluence/display/JFROG/JFrog+IntelliJ+IDEA+Plugin"));
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
}