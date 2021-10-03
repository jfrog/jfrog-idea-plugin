package com.jfrog.ide.idea.utils;

import com.google.common.base.Objects;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public static final Path HOME_PATH = Paths.get(System.getProperty("user.home"), ".jfrog-idea-plugin");

    public static Path getProjectBasePath(Project project) {
        return project.getBasePath() != null ? Paths.get(project.getBasePath()) : Paths.get(".");
    }

    public static boolean areRootNodesEqual(DependencyTree lhs, DependencyTree rhs) {
        GeneralInfo lhsGeneralInfo = lhs.getGeneralInfo();
        GeneralInfo rhsGeneralInfo = rhs.getGeneralInfo();
        return ObjectUtils.allNotNull(lhsGeneralInfo, rhsGeneralInfo) &&
                StringUtils.equals(lhsGeneralInfo.getName(), rhsGeneralInfo.getName()) &&
                StringUtils.equals(lhsGeneralInfo.getPath(), rhsGeneralInfo.getPath()) &&
                StringUtils.equals(lhsGeneralInfo.getPkgType(), rhsGeneralInfo.getPkgType());
    }

    public static int getProjectIdentifier(String name, String path) {
        return Objects.hashCode(name, path);
    }

    public static int getProjectIdentifier(Project project) {
        return getProjectIdentifier(project.getName(), project.getBasePath());
    }

    public static void focusJFrogToolWindow(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JFrog");
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }
}
