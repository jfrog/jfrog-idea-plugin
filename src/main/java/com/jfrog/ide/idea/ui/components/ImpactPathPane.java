package com.jfrog.ide.idea.ui.components;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Severity;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Stack;

/**
 * @author yahavi
 **/
public class ImpactPathPane extends JComponent {
    public ImpactPathPane(DependencyTree impactedNode, Severity severity) {
        setBackground(UIUtil.getTableBackground());
        setLayout(new GridBagLayout());
        add(Box.createRigidArea(new Dimension(0, 30)));
        addContent(extractImpactedPath(impactedNode), severity);
    }

    /**
     * Extract the impact path from the dependency tree into a stack.
     *
     * @param impactedNode - The impacted node
     * @return the impact path.
     */
    private Stack<String> extractImpactedPath(DependencyTree impactedNode) {
        Stack<String> rootImpactPath = new Stack<>();
        for (DependencyTree node = impactedNode; node.getParent() != null && !node.isMetadata();
             node = (DependencyTree) node.getParent()) {
            rootImpactPath.add(node.toString());
        }

        return rootImpactPath;
    }

    /**
     * Add the components to the impact path panel.
     *
     * @param impactPath - Impact path in reverse order
     * @param severity   - The issue severity
     */
    private void addContent(Stack<String> impactPath, Severity severity) {
        GridBagConstraints constraints = createConstraints();
        Color borderColor = getBorderColor(severity);
        while (!impactPath.isEmpty()) {
            // Add component label
            JLabel componentLabel = createComponentLabel(borderColor, impactPath.pop());
            add(componentLabel, constraints);
            constraints.gridy++;

            if (impactPath.isEmpty()) {
                // Set icon and emphasize the last element
                componentLabel.setIcon(IconUtils.load(StringUtils.lowerCase(severity.name())));
                componentLabel.setFont(JBFont.label().asBold());
            } else {
                // Add arrow label
                JLabel arrowLabel = createArrowLabel(borderColor);
                add(arrowLabel, constraints);
                constraints.gridy++;
            }
        }
    }

    private GridBagConstraints createConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new JBInsets(0, 0, 0, 100);
        constraints.gridx = 1;
        constraints.gridy = 1;
        return constraints;
    }

    private JLabel createArrowLabel(Color borderColor) {
        JLabel arrowLabel = new JBLabel("⇣");
        arrowLabel.setFont(JBFont.label().biggerOn(5));
        arrowLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        arrowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        arrowLabel.setForeground(borderColor);
        return arrowLabel;
    }

    private JLabel createComponentLabel(Color borderColor, String componentId) {
        JLabel componentLabel = new JBLabel(componentId);
        componentLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        componentLabel.setHorizontalAlignment(SwingConstants.CENTER);
        componentLabel.setFont(JBFont.label());
        componentLabel.setBorder(new ImpactedComponentBorder(borderColor));
        return componentLabel;
    }

    @SuppressWarnings("UseJBColor")
    private Color getBorderColor(Severity severity) {
        switch (severity) {
            case Critical:
                return new Color(194, 17, 3);
            case High:
                return new Color(249, 126, 58);
            case Medium:
                return new Color(255, 195, 0);
            case Low:
                return new Color(211, 249, 167);
            case Unknown:
                return new Color(143, 139, 155);
        }
        return null;
    }

    /**
     * Represents the rounded corners border around the components in the impacted path panel.
     */
    private static class ImpactedComponentBorder implements Border {
        private static final int RADIUS = 20;
        private final Color color;

        private ImpactedComponentBorder(Color borderColor) {
            this.color = borderColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return JBUI.insets(RADIUS + 1, RADIUS + 1, RADIUS + 2, RADIUS);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            g.drawRoundRect(x, y, width - 1, height - 1, RADIUS, RADIUS);
        }
    }
}
