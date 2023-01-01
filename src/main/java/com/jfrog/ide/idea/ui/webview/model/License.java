package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class License implements Serializable {
    String name;
    String link;

    public License(String name, String link) {
        this.name = name;
        this.link = link;
    }

    public License() {
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }
}
