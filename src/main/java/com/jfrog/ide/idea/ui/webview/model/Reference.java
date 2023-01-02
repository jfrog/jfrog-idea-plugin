package com.jfrog.ide.idea.ui.webview.model;

public class Reference {
    private final String url;
    private final String text;

    public Reference(String url, String text) {
        this.url = url;
        this.text = text;
    }

    @SuppressWarnings("unused")
    public String getUrl() {
        return url;
    }

    @SuppressWarnings("unused")
    public String getText() {
        return text;
    }
}
