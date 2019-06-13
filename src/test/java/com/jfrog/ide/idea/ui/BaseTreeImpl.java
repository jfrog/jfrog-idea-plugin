package com.jfrog.ide.idea.ui;

import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.NpmProject;

/**
 * @author yahavi
 */
public class BaseTreeImpl extends BaseTree {

    BaseTreeImpl() {
        super(new NpmProject("."));
    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectName) {

    }
}
