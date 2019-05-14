package com.jfrog.ide.idea.utils;

import com.intellij.openapi.project.Project;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;

import javax.swing.event.TreeExpansionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static Path getProjectBasePath(Project project) {
        return project.getBasePath() != null ? Paths.get(project.getBasePath()) : Paths.get(".");
    }
}
