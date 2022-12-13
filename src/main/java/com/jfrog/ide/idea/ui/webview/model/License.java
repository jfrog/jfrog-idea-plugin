package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class License implements Serializable {
    String name;

    public License(String name) {
        this.name = name;
    }
    public License() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
