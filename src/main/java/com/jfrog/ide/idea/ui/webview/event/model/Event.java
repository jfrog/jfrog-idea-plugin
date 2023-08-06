package com.jfrog.ide.idea.ui.webview.event.model;

import java.io.Serializable;

/**
 * The Event class is an abstract model designed to facilitate communication between the IDE and the Webview.
 * It serves as the base class for various event types that can be transmitted between these components.
 */
public abstract class Event implements Serializable {
    private Object data;

    public Event(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}