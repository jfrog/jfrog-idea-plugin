package com.jfrog.ide.idea.ui.jcef.message;

import java.io.Serializable;

public class PackedMessage implements Serializable {

    public static final String BROWSER_SEND_FUNCTION_NAME = "sendMessageToBrowser";
    public static final String IDE_SEND_FUNCTION_NAME = "sendMessageToIde";

    private Object data;
    private String type;

    public PackedMessage() {
        this.data = null;
        this.type = "";
    }

    public PackedMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }
}
