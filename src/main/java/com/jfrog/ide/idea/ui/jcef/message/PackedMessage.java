package com.jfrog.ide.idea.ui.jcef.message;

import java.io.Serializable;

public class PackedMessage implements Serializable {

    public static final String BROWSER_SEND_FUNCTION_NAME = "sendMessageToBrowser";
    public static final String IDE_SEND_FUNCTION_NAME = "sendMessageToIde";

    private Object data;
    private String pageType;

    public PackedMessage() {
        this.data = null;
        this.pageType = "";
    }

    public PackedMessage(String type, Object data) {
        this.pageType = type;
        this.data = data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public Object getData() {
        return data;
    }
}
