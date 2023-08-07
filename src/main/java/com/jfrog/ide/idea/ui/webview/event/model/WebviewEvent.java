package com.jfrog.ide.idea.ui.webview.event.model;

/**
 * Represents a Webview-specific event that can be sent from the IDE to the Webview.
 */
public class WebviewEvent extends Event {
    private Type type;

    @SuppressWarnings("unused")
    public WebviewEvent() {
        super(null);
    }

    public WebviewEvent(Type type, Object data) {
        super(data);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {SET_EMITTER, SHOW_PAGE}
}

