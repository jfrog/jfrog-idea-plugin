package com.jfrog.ide.idea.events;

import com.intellij.util.messages.Topic;
import com.jfrog.ide.common.ci.BuildGeneralInfo;

/**
 * Project based events.
 *
 * @author yahavi
 */
public interface BuildEvents {
    Topic<BuildEvents> ON_SELECTED_BUILD = Topic.create("Build selected", BuildEvents.class);

    /**
     * Called when the the selected build is modified.
     */
    void update(BuildGeneralInfo generalInfo);
}
