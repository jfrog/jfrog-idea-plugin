package com.jfrog.ide.idea.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ClickListener;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import com.jfrog.ide.idea.ui.filters.FilterMenu;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class FilterButton extends JPanel {
    private static final int GAP_BEFORE_ARROW = 3;
    private static final int BORDER_SIZE = 2;
    private JLabel myNameLabel;
    private FilterMenu filterMenu;

    public FilterButton(FilterMenu filterMenu, String myName, String toolTip) {
        this.filterMenu = filterMenu;
        initUi(myName, toolTip);
    }

    private void initUi(String myName, String toolTip) {
        myNameLabel = new JBLabel(myName);
        myNameLabel.setIcon(AllIcons.General.Filter);
        JLabel arrow = new JBLabel(AllIcons.Ide.Statusbar_arrows);
        setToolTipText(toolTip);
        setDefaultForeground();
        setFocusable(true);
        setBorder(createUnfocusedBorder());

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(myNameLabel);
        add(Box.createHorizontalStrut(GAP_BEFORE_ARROW));
        add(arrow);

        revalidate();
        repaint();
        showPopupMenuOnClick();
        indicateHovering();
        indicateFocusing();
    }

    /**
     * Create popup actions available under this filter.
     */
    private void indicateFocusing() {
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(@NotNull FocusEvent e) {
                setBorder(createFocusedBorder());
            }

            @Override
            public void focusLost(@NotNull FocusEvent e) {
                setBorder(createUnfocusedBorder());
            }
        });
    }

    private void showPopupMenuOnClick() {
        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                showPopupMenu();
                return true;
            }
        }.installOn(myNameLabel);
    }

    private void indicateHovering() {
        myNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(@NotNull MouseEvent e) {
                setOnHoverForeground();
            }

            @Override
            public void mouseExited(@NotNull MouseEvent e) {
                setDefaultForeground();
            }
        });
    }

    private void setDefaultForeground() {
        myNameLabel.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getHeaderInactiveColor() : UIUtil.getInactiveTextColor());
    }

    private void setOnHoverForeground() {
        myNameLabel.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getHeaderActiveColor() : UIUtil.getTextAreaForeground());
    }

    private void showPopupMenu() {
        filterMenu.show(this, myNameLabel.getBounds().x, myNameLabel.getBounds().y + myNameLabel.getBounds().height);
    }

    private static Border createFocusedBorder() {
        return BorderFactory.createCompoundBorder(new RoundedLineBorder(UIUtil.getHeaderActiveColor(), 10, BORDER_SIZE), JBUI.Borders.empty(2));
    }

    private static Border createUnfocusedBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE), JBUI.Borders.empty(2));
    }
}