package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.jfrog.ide.idea.utils.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;

/**
 * Expand all action that calls treeExpanded() once in the end instead of for each row.
 *
 * Created by Yahav Itzhak on 3 Jan 2018.
 */
public class ExpandAllAction extends AnAction implements DumbAware {

    private JTree myTree;

    private ExpandAllAction() {
        super("Expand All", "Expand All", AllIcons.Actions.Expandall);
    }

    public ExpandAllAction(@NotNull JTree tree) {
        this();
        this.myTree = tree;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        TreeExpansionListener treeExpansionListener = Utils.getIssuesTreeExpansionListener(myTree.getTreeExpansionListeners());

        if (treeExpansionListener != null) {
            myTree.removeTreeExpansionListener(treeExpansionListener);
        }

        for (int i = 0; i < myTree.getRowCount(); i++) {
            myTree.expandRow(i);
        }

        if (treeExpansionListener != null) {
            myTree.addTreeExpansionListener(treeExpansionListener);
            treeExpansionListener.treeExpanded(null);
        }
    }
}
