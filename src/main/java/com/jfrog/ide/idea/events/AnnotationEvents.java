package com.jfrog.ide.idea.events;

import com.intellij.util.messages.Topic;

public interface AnnotationEvents {
    // Results expiry
    Topic<AnnotationEvents> ON_IRRELEVANT_RESULT = Topic.create("Source code changed", AnnotationEvents.class);

    /**
     * Called when the selected file is modified.
     */
    void update(String filePath);
}
