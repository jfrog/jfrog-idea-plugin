package com.jfrog.ide.idea.utils;

import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;

import javax.swing.event.TreeExpansionListener;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public static TreeExpansionListener getIssuesTreeExpansionListener(TreeExpansionListener[] treeExpansionListeners) {
        if (treeExpansionListeners == null) {
            return null;
        }
        for (TreeExpansionListener treeExpansionListener : treeExpansionListeners) {
            if (treeExpansionListener instanceof IssuesTreeExpansionListener) {
                return treeExpansionListener;
            }
        }
        return null;
    }
}
