package org.jfrog.idea.ui.xray.filters;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.ClickListener;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.*;

/**
 * Created by romang on 4/13/17.
 */
public abstract class FilterMenu extends JPanel implements ItemListener {
    private static final int GAP_BEFORE_ARROW = 3;
    private static final int BORDER_SIZE = 2;

    @NotNull
    private final String myName;
    @NotNull
    private JLabel myNameLabel;
    @NotNull
    private ListPopup popup;

    protected FilterMenu(String name) {
        myName = name;
        initUi();
    }

    public void initUi() {
        myNameLabel = new JLabel(myName);
        setDefaultForeground();
        setFocusable(true);
        setBorder(createUnfocusedBorder());

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(myNameLabel);
        add(Box.createHorizontalStrut(GAP_BEFORE_ARROW));
        add(new JLabel(AllIcons.Ide.Statusbar_arrows));

        showPopupMenuOnClick();
        showPopupMenuFromKeyboard();
        indicateHovering();
        indicateFocusing();
    }

    protected abstract DefaultActionGroup createActionGroup();

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

    private void showPopupMenuFromKeyboard() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(@NotNull KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    showPopupMenu();
                }
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
        }.installOn(this);
    }

    private void indicateHovering() {
        addMouseListener(new MouseAdapter() {
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
        myNameLabel.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getLabelForeground() : UIUtil.getInactiveTextColor());
    }

    private void setOnHoverForeground() {
        myNameLabel.setForeground(UIUtil.isUnderDarcula() ? UIUtil.getLabelForeground() : UIUtil.getTextAreaForeground());
    }

    private void showPopupMenu() {
        popup = createPopupMenu();
        popup.showUnderneathOf(this);
    }

    @NotNull
    protected ListPopup createPopupMenu() {
        return JBPopupFactory.getInstance().
                createActionGroupPopup(null, createActionGroup(), DataManager.getInstance().getDataContext(this),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    private static Border createFocusedBorder() {
        return BorderFactory.createCompoundBorder(new RoundedLineBorder(UIUtil.getHeaderActiveColor(), 10, BORDER_SIZE), JBUI.Borders.empty(2));
    }

    private static Border createUnfocusedBorder() {
        return BorderFactory
                .createCompoundBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE), JBUI.Borders.empty(2));
    }

    @Override
    public void itemStateChanged(ItemEvent e) {

    }
}
