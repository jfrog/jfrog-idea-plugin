package com.jfrog.ide.idea.utils;

import com.google.common.base.Objects;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.jfrog.ide.idea.ui.listeners.IssuesTreeExpansionListener;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

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

    public static boolean areRootNodesEqual(DependenciesTree lhs, DependenciesTree rhs) {
        GeneralInfo lhsGeneralInfo = lhs.getGeneralInfo();
        GeneralInfo rhsGeneralInfo = rhs.getGeneralInfo();
        return StringUtils.equals(lhsGeneralInfo.getName(), rhsGeneralInfo.getName()) &&
                StringUtils.equals(lhsGeneralInfo.getPath(), rhsGeneralInfo.getPath());
    }

    public static int getProjectIdentifier(String name, String path) {
        return Objects.hashCode(name, path);
    }

    public static int getProjectIdentifier(Project project) {
        return getProjectIdentifier(project.getName(), project.getBasePath());
    }

    public static void focusJFrogToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JFrog");
        ((ToolWindowImpl) toolWindow).fireActivated();
    }
}
