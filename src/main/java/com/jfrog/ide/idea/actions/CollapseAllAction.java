package com.jfrog.ide.idea.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionListener;

import static com.jfrog.ide.idea.utils.Utils.getIssuesTreeExpansionListener;

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
    public void actionPerformed(AnActionEvent e) {
        TreeExpansionListener treeExpansionListener = getIssuesTreeExpansionListener(myTree.getTreeExpansionListeners());

        if (treeExpansionListener != null) {
            myTree.removeTreeExpansionListener(treeExpansionListener);
        }

        for (int i = myTree.getRowCount() - 1; i >= 0; i--) {
            myTree.collapseRow(i);
        }

        if (treeExpansionListener != null) {
            myTree.addTreeExpansionListener(treeExpansionListener);
            treeExpansionListener.treeCollapsed(null);
        }
    }
}
