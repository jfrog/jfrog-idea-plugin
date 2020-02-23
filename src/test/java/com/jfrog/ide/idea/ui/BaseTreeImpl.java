package com.jfrog.ide.idea.ui;

import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.utils.ProjectsMap;
import com.jfrog.ide.idea.projects.NpmProject;

/**
 * @author yahavi
 */
public class BaseTreeImpl extends BaseTree {

    BaseTreeImpl() {
        super(new NpmProject("."));
    }

    @Override
    protected void addOnProjectChangeListener(MessageBusConnection busConnection) {

    }

    @Override
    public void applyFilters(ProjectsMap.ProjectKey projectName) {

    }
}
