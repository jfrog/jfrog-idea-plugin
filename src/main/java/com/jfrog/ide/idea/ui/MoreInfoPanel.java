package com.jfrog.ide.idea.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

import static com.jfrog.ide.idea.ui.utils.ComponentUtils.createJTextArea;
import static com.jfrog.ide.idea.ui.utils.ComponentUtils.replaceAndUpdateUI;

/**
 * Represents the right "More Info" panel.
 *
 * @author yahavi
 */
public class MoreInfoPanel extends JPanel {
    protected int lastTextPosition = 0;

    public MoreInfoPanel() {
        setLayout(new GridBagLayout());
        setBackground(UIUtil.getTableBackground());
    }

    protected void addComponent(String header, JComponent component) {
        JLabel headerLabel = createHeaderLabel(header + ":");
        GridBagConstraints gridBagConstraints = createGridBagConstraints();

        add(headerLabel, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.9;
        add(component, gridBagConstraints);
    }

    protected void addText(String header, String text) {
        if (StringUtils.isNotBlank(text)) {
            addComponent(header, createJTextArea(text, true));
        }
    }

    protected JLabel createHeaderLabel(String title) {
        JLabel headerLabel = new JBLabel(title);
        headerLabel.setOpaque(true);
        headerLabel.setBackground(UIUtil.getTableBackground());
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        return headerLabel;
    }

    protected GridBagConstraints createGridBagConstraints() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.gridy = lastTextPosition++;
        return gridBagConstraints;
    }

    protected static void createComponentInfoNotAvailablePanel(JPanel panel) {
        replaceAndUpdateUI(panel, ComponentUtils.createDisabledTextLabel("Component information is not available"), BorderLayout.CENTER);
    }
}
