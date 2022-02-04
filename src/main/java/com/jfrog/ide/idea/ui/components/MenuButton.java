package com.jfrog.ide.idea.ui.components;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.ClickListener;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.jfrog.ide.idea.ui.configuration.Utils.setActiveForegroundColor;
import static com.jfrog.ide.idea.ui.configuration.Utils.setInactiveForegroundColor;

/**
 * Created by Yahav Itzhak on 22 Nov 2017.
 */
public class MenuButton extends JPanel {
    private static final int GAP_BEFORE_ARROW = 3;
    private static final int BORDER_SIZE = 2;
    private JLabel myNameLabel;
    private final JBPopupMenu filterMenu;
    private boolean filterEnabled;

    public MenuButton(JBPopupMenu filterMenu, String myName, String toolTip, Icon icon) {
        this.filterMenu = filterMenu;
        myNameLabel = new JBLabel(myName);
        myNameLabel.setIcon(icon);
        initUi(toolTip);
    }

    private void initUi(String toolTip) {
        JLabel arrow = new JBLabel(AllIcons.Ide.Statusbar_arrows);
        setToolTipText(toolTip);
        setInactiveForegroundColor(myNameLabel);
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
     * If one of the filters is applied (a checkbox is unselected), show this in the UI.
     *
     * @param filterEnabled - True if one of the filters is applied.
     */
    public void indicateFilterEnable(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
        if (filterEnabled) {
            setActiveForegroundColor(myNameLabel);
        } else {
            setInactiveForegroundColor(myNameLabel);
        }
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
                if (!filterEnabled) {
                    setActiveForegroundColor(myNameLabel);
                }
            }

            @Override
            public void mouseExited(@NotNull MouseEvent e) {
                if (!filterEnabled) {
                    setInactiveForegroundColor(myNameLabel);
                }
            }
        });
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