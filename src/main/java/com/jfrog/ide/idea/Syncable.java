package com.jfrog.ide.idea;

import com.intellij.util.messages.Topic;
import com.jfrog.ide.idea.events.ApplicationEvents;

public interface Syncable {
    Topic<ApplicationEvents> getSyncEvent();
}
