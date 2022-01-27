package com.jfrog.ide.idea.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.jfrog.ide.idea.utils.Utils.isValidUrl;

/**
 * Represents the expandable references view at the issue details panel.
 *
 * @author yahavi
 **/
public class ReferencesPane extends JPanel {
    private final JButton collapseExpandButton = new JButton();
    private final Icon collapseIcon = AllIcons.Actions.MoveUp;
    private final Icon expandIcon = AllIcons.Actions.MoveDown;
    private final JPanel references = new JBPanel<>();
    private boolean collapsed;

    public ReferencesPane(List<String> references) {
        super(new GridBagLayout());
        initReferences(references);
        initButton();
        setCollapsed(true);
        setFocusable(false);
        setBackground(UIUtil.getTableBackground());
    }

    /**
     * Init the references panel according to the input list of references.
     *
     * @param references - The references list
     */
    @SuppressWarnings("UnstableApiUsage")
    private void initReferences(List<String> references) {
        for (String reference : references) {
            if (!isValidUrl(reference)) {
                continue;
            }
            HyperlinkLabel referenceLabel = new HyperlinkLabel();
            referenceLabel.setTextWithHyperlink("<hyperlink>" + reference + "</hyperlink>");
            referenceLabel.addHyperlinkListener(e -> BrowserUtil.browse(reference));
            this.references.add(referenceLabel);
        }
        this.references.setLayout(new GridLayout(0, 1));
    }

    /**
     * Init the collapse-expand button.
     */
    private void initButton() {
        Dimension buttonDimension = new JBDimension(expandIcon.getIconWidth(), expandIcon.getIconHeight());

        collapseExpandButton.setOpaque(false);
        collapseExpandButton.setBorderPainted(false);
        collapseExpandButton.setSize(buttonDimension);
        collapseExpandButton.setPreferredSize(buttonDimension);
        collapseExpandButton.setMinimumSize(buttonDimension);
        collapseExpandButton.setMaximumSize(buttonDimension);
        collapseExpandButton.addActionListener(e -> setCollapsed(!collapsed));
        add(collapseExpandButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                JBUI.insets(-5, 0, 0, -5), 0, 0));
    }

    /**
     * Collapse or expand the references section.
     *
     * @param collapsed - True to collapse, false to expand
     */
    private void setCollapsed(boolean collapsed) {
        if (collapsed) {
            remove(references);
        } else {
            add(references, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
        }
        collapseExpandButton.setIcon(collapsed ? expandIcon : collapseIcon);
        collapseExpandButton.setToolTipText(collapsed ? "Show references" : "Hide references");

        this.collapsed = collapsed;
        revalidate();
        repaint();
    }
}
