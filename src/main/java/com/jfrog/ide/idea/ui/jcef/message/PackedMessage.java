package com.jfrog.ide.idea.ui.jcef.message;

import java.io.Serializable;

public class PackedMessage implements Serializable {
    private Object pageData;
    private String type;

    public PackedMessage() {
        this.pageData = null;
    }

    public PackedMessage(MessageType type, Object data) {
        this.pageData = data;
        this.type = type.getValue();
    }

    public Object getPageData() {
        return pageData;
    }

    public void setPageData(String pageData) {
        this.pageData = pageData;
    }

    public String getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type.getValue();
    }
}
