package com.jfrog.ide.idea.ui.components;

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.JXCollapsiblePane;

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
    private final JXCollapsiblePane references = new JXCollapsiblePane(JXCollapsiblePane.Direction.DOWN);

    public ReferencesPane(List<String> references) {
        super(new GridBagLayout());
        GridBagConstraints constraints = createConstraints();
        initButton(constraints);
        constraints.gridy++;
        initReferences(references, constraints);
        setFocusable(false);
        setBackground(UIUtil.getTableBackground());
    }

    /**
     * Create the grid bag constraints for the button and the references panel.
     *
     * @return grid bag constraints.
     */
    private GridBagConstraints createConstraints() {
        return new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                JBUI.insets(-5, 0, 0, -5), 0, 0);
    }

    /**
     * Init the references panel according to the input list of references.
     *
     * @param references  - The references list
     * @param constraints - The grib bag constraints
     */
    @SuppressWarnings("UnstableApiUsage")
    private void initReferences(List<String> references, GridBagConstraints constraints) {
        this.references.setCollapsed(true);
        for (String reference : references) {
            if (!isValidUrl(reference)) {
                continue;
            }
            HyperlinkLabel referenceLabel = new HyperlinkLabel();
            referenceLabel.setTextWithHyperlink("<hyperlink>" + reference + "</hyperlink>");
            referenceLabel.addHyperlinkListener(e -> BrowserUtil.browse(reference));
            this.references.add(referenceLabel);
        }
        add(this.references, constraints);
    }

    /**
     * Init the collapse-expand button.
     *
     * @param constraints - The grib bag constraints
     */
    private void initButton(GridBagConstraints constraints) {
        JButton collapseExpandButton = new JButton(references.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
        collapseExpandButton.setText("Show/Hide References");
        collapseExpandButton.setBorderPainted(false);

        Action toggleAction = references.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION);
        toggleAction.putValue(JXCollapsiblePane.COLLAPSE_ICON, UIManager.getIcon("Tree.expandedIcon"));
        toggleAction.putValue(JXCollapsiblePane.EXPAND_ICON, UIManager.getIcon("Tree.collapsedIcon"));

        add(collapseExpandButton, constraints);
    }
}
