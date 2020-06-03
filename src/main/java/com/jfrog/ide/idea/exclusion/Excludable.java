package com.jfrog.ide.idea.exclusion;

import com.intellij.openapi.project.Project;

/**
 * Created by Bar Belity on 28/05/2020.
 */
public interface Excludable {

    /**
     * Exclude from project-descriptor.
     */
    void exclude(Project project);

}
