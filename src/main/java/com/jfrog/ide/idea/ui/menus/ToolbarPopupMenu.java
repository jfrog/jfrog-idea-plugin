package com.jfrog.ide.idea.ui.menus;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.jfrog.ide.idea.ui.components.MenuButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Represents a toolbar menu. Base class for all filter and export menus.
 *
 * @author yahavi
 **/
public abstract class ToolbarPopupMenu extends JBPopupMenu {
    protected final MenuButton menuButton;
    protected final Project project;

    public ToolbarPopupMenu(@NotNull Project project, String name, String tooltip, Icon icon) {
        this.project = project;
        this.menuButton = new MenuButton(this, name, tooltip, icon);
    }

    public MenuButton getMenuButton() {
        return menuButton;
    }

    /**
     * Refresh the menu items. Invoked after a change in the dependency tree.
     */
    public abstract void refresh();
}
