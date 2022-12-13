package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class Reference implements Serializable {
    String url;
    String text;

    public Reference(String url, String text) {
        this.url = url;
        this.text = text;
    }
    
    public Reference() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
