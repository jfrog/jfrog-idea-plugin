package com.jfrog.ide.idea.ui.webview.event.model;

/**
 * Represents an IDE-specific event that can be sent from the IDE to the Webview.
 */
public class IdeEvent extends Event {
    private Type type;

    public IdeEvent() {
        super(null);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {SHOW_CODE}
}