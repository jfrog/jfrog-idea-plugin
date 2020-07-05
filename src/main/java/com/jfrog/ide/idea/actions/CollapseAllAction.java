package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Collapse all action that calls treeCollapsed() once in the end instead of for each row.
 *
 * Created by Yahav Itzhak on 3 Jan 2018.
 */
public class CollapseAllAction extends AnAction implements DumbAware {

    private JTree myTree;

    private CollapseAllAction() {
        super("Collapse All", "Collapse All", AllIcons.Actions.Collapseall);
    }

    public CollapseAllAction(@NotNull JTree tree) {
        this();
        this.myTree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = myTree.getRowCount() - 1; i >= 0; i--) {
            myTree.collapseRow(i);
        }
    }
}
