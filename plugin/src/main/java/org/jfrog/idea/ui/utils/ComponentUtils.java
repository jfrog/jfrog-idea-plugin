package org.jfrog.idea.ui.utils;

import com.intellij.ui.components.JBLabel;

import javax.swing.*;

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
        jTextArea.setBackground(null);
        return jTextArea;
    }

    public static JBLabel createDisabledTextLabel(String text) {
        JBLabel label = new JBLabel(text);
        label.setEnabled(false);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}
