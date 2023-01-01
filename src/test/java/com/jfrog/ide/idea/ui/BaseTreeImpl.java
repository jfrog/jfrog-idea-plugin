package com.jfrog.ide.idea.ui;

import com.intellij.mock.MockProject;
import com.intellij.util.messages.MessageBusConnection;
import com.jfrog.ide.common.utils.ProjectsMap;

/**
 * @author yahavi
 */
public class BaseTreeImpl extends ComponentsTree {

    BaseTreeImpl() {
        super(new MockProject(null, () -> {
        }));
    }

    @Override
    public void addOnProjectChangeListener(MessageBusConnection busConnection) {
    }

    @Override
    public void applyFiltersForAllProjects() {
    }
}
