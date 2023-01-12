package com.jfrog.ide.idea.ui.webview.model;

public class License {
    private final String name;
    private final String link;

    public License(String name, String link) {
        this.name = name;
        this.link = link;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public String getLink() {
        return link;
    }
}
