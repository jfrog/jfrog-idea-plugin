package com.jfrog.ide.idea.ui.webview.model;

public class ImpactGraph {
    private final ImpactGraphNode root;
    private final int pathsCount;
    private final int pathsLimit;

    public ImpactGraph(ImpactGraphNode root, int pathsCount, int pathsLimit) {
        this.root = root;
        this.pathsCount = pathsCount;
        this.pathsLimit = pathsLimit;
    }

    @SuppressWarnings("unused")
    public ImpactGraphNode getRoot() {
        return root;
    }

    @SuppressWarnings("unused")
    public int getPathsCount() {
        return pathsCount;
    }

    @SuppressWarnings("unused")
    public int getPathsLimit() {
        return pathsLimit;
    }
}
