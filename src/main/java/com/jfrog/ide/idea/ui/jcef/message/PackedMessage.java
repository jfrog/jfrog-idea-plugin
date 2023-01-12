package com.jfrog.ide.idea.ui.jcef.message;

import java.io.Serializable;

public class PackedMessage implements Serializable {

    public static final String BROWSER_SEND_FUNCTION_NAME = "sendMessageToBrowser";
    public static final String IDE_SEND_FUNCTION_NAME = "sendMessageToIde";

    private Object data;

    public PackedMessage() {
        this.data = null;
    }

    public PackedMessage(Object data) {
        this.data = data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
