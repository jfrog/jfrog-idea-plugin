package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Expand all action that calls treeExpanded() once in the end instead of for each row.
 *
 * Created by Yahav Itzhak on 3 Jan 2018.
 */
public class ExpandAllAction extends AnAction implements DumbAware {

    private JTree myTree;

    @SuppressWarnings("DialogTitleCapitalization")
    private ExpandAllAction() {
        super("Expand All", "Expand All", AllIcons.Actions.Expandall);
    }

    public ExpandAllAction(@NotNull JTree tree) {
        this();
        this.myTree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        for (int i = 0; i < myTree.getRowCount(); i++) {
            myTree.expandRow(i);
        }
    }
}
