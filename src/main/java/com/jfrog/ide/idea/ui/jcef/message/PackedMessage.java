package com.jfrog.ide.idea.ui.jcef.message;

import java.io.Serializable;

public class PackedMessage implements Serializable {
    private Object pageData;
    private String type;

    public PackedMessage(MessageType type, Object data) {
        this.pageData = data;
        this.type = type.toString();
    }

    public Object getPageData() {
        return pageData;
    }

    public void setPageData(String data) {
        this.pageData = data;
    }

    public String getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type.toString();
    }
}
