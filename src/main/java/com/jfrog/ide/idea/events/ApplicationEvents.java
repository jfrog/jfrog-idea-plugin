package com.jfrog.ide.idea.events;

import com.intellij.util.messages.Topic;

/**
 * Application based events.
 * <p>
 * Created by romang on 3/5/17.
 */
public interface ApplicationEvents {
    // Scan started
    Topic<ApplicationEvents> ON_SCAN_LOCAL_STARTED = Topic.create("Local scan started", ApplicationEvents.class);
    Topic<ApplicationEvents> ON_SCAN_CI_STARTED = Topic.create("CI scan started", ApplicationEvents.class);

    // Configuration changed
    Topic<ApplicationEvents> ON_CONFIGURATION_DETAILS_CHANGE = Topic.create("Configuration details changed", ApplicationEvents.class);
    Topic<ApplicationEvents> ON_BUILDS_CONFIGURATION_CHANGE = Topic.create("Builds configuration changed", ApplicationEvents.class);

    // Filter changed
    Topic<ApplicationEvents> ON_SCAN_FILTER_CHANGE = Topic.create("Scan issues changed", ApplicationEvents.class);
    Topic<ApplicationEvents> ON_CI_FILTER_CHANGE = Topic.create("CI issues changed", ApplicationEvents.class);

    /**
     * Called when a scan started, a configuration changed or a filter changed.
     */
    void update();

}
