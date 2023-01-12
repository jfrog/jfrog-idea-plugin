package com.jfrog.ide.idea.ui.webview.model;

public class ImpactedPath {
    private final String name;
    private final ImpactedPath[] children;

    public ImpactedPath(String name, ImpactedPath[] children) {
        this.name = name;
        this.children = children;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public ImpactedPath[] getChildren() {
        return children;
    }
}
