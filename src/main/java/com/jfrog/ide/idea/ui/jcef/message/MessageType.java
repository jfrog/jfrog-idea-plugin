package com.jfrog.ide.idea.ui.jcef.message;

public enum MessageType {
    SetEmitter("SET_EMITTER"),
    ShowPage("SHOW_PAGE");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
