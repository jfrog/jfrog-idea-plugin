package com.jfrog.ide.idea.ui.jcef.message;

/**
 * Defines base interface for message channel implementation.
 * Both IDE and browser implementations must be in sync with each other.
 */
public interface MessagePipe {
    /**
     * Sends raw message with type [type] and content [data] to the other end.
     */
    void send(String type, Object data);
}
