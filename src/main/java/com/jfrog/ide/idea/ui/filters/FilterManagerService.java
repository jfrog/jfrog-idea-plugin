package com.jfrog.ide.idea.ui.filters;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.jfrog.ide.common.filter.FilterManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author yahavi
 */
public class FilterManagerService extends FilterManager {

    public static FilterManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, FilterManagerService.class);
    }
}
