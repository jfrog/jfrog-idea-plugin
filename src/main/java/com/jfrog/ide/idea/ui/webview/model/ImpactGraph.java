package com.jfrog.ide.idea.ui.webview.model;

public class ImpactGraph {
    private final ImpactGraphNode root;
    private final int pathsLimit;

    public ImpactGraph(ImpactGraphNode root, int pathsLimit) {
        this.root = root;
        this.pathsLimit = pathsLimit;
    }

    @SuppressWarnings("unused")
    public ImpactGraphNode getRoot() {
        return root;
    }

    @SuppressWarnings("unused")
    public int getPathsLimit() {
        return pathsLimit;
    }
}
