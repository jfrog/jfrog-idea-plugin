package com.jfrog.ide.idea.events;

import com.intellij.util.messages.Topic;

/**
 * Application based events.
 * <p>
 * Created by romang on 3/5/17.
 */
public interface ApplicationEvents {
    Topic<ApplicationEvents> ON_CONFIGURATION_DETAILS_CHANGE = Topic.create("Configuration details changed", ApplicationEvents.class);
    Topic<ApplicationEvents> ON_SCAN_FILTER_CHANGE = Topic.create("Scan issues changed", ApplicationEvents.class);

    /**
     * Called when the store of issues in changed files is modified. It is modified only as a result of a user action to analyse all changed files.
     */
    void update();

}
