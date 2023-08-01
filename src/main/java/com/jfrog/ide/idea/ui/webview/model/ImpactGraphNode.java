package com.jfrog.ide.idea.ui.webview.model;

public class ImpactGraphNode {
    private final String name;
    private final ImpactGraphNode[] children;

    public ImpactGraphNode(String name, ImpactGraphNode[] children) {
        this.name = name;
        this.children = children;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public ImpactGraphNode[] getChildren() {
        return children;
    }
}
