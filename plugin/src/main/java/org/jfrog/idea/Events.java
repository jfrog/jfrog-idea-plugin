package org.jfrog.idea;

import com.intellij.util.messages.Topic;

/**
 * Created by romang on 3/5/17.
 */
public interface Events {
    Topic<Events> ON_CONFIGURATION_DETAILS_CHANGE = Topic.create("Configuration details changed", Events.class);
    Topic<Events> ON_SCAN_COMPONENTS_CHANGE = Topic.create("Component view changed", Events.class);
    Topic<Events> ON_SCAN_FILTER_CHANGE = Topic.create("Scan filter changed", Events.class);
    Topic<Events> ON_SCAN_ISSUES_CHANGE = Topic.create("Scan issues changed", Events.class);

    /**
     * Called when the store of issues in changed files is modified. It is modified only as a result of a user action to analyse all changed files.
     */
    void update();

}
