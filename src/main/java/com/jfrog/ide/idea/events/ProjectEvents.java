package com.jfrog.ide.idea.events;

import com.intellij.util.messages.Topic;
import com.jfrog.ide.common.utils.ProjectsMap;

/**
 * Project based events.
 *
 * @author yahavi
 */
public interface ProjectEvents {
    Topic<ProjectEvents> ON_SCAN_PROJECT_CHANGE = Topic.create("Scan changed", ProjectEvents.class);
    Topic<ProjectEvents> ON_SCAN_CI_CHANGE = Topic.create("CI changed", ProjectEvents.class);

    /**
     * Called when the store of issues in changed files is modified. It is modified only as a result of a user action to analyse all changed files.
     */
    void update(ProjectsMap.ProjectKey projectKey);
}
