package com.jfrog.ide.idea.npm;

import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectImpl;

/**
 * @author yahavi
 */
public class NpmProject extends ProjectImpl {

    public NpmProject(String name, String basePath) {
        super(ProjectManager.getInstance(), basePath, name);
    }

}
