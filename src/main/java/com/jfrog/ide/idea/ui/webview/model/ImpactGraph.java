package com.jfrog.ide.idea.ui.webview.model;

public class ImpactGraph {
    private final String name;
    private final ImpactGraph[] children;

    public ImpactGraph(String name, ImpactGraph[] children) {
        this.name = name;
        this.children = children;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public ImpactGraph[] getChildren() {
        return children;
    }
}
