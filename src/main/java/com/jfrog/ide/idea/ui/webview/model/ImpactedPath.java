package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class ImpactedPath implements Serializable {
    String name;
    ImpactedPath[] children;

    public ImpactedPath() {
    }

    public ImpactedPath(String name, ImpactedPath[] children) {
        this.name = name;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImpactedPath[] getChildren() {
        return children;
    }

    public void setChildren(ImpactedPath[] children) {
        this.children = children;
    }
}
