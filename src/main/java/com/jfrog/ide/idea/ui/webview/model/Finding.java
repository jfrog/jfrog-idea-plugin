package com.jfrog.ide.idea.ui.webview.model;

public class Finding {
    private final String does;
    private final String happen;
    private final String meaning;
    private final String snippet;

    public Finding(String happen, String meaning, String snippet, String does) {
        this.happen = happen;
        this.meaning = meaning;
        this.snippet = snippet;
        this.does = does;
    }

    public Finding(Finding other) {
        this(other.happen, other.meaning, other.snippet, other.does);
    }

    @SuppressWarnings("unused")
    public String getHappen() {
        return happen;
    }

    @SuppressWarnings("unused")
    public String getMeaning() {
        return meaning;
    }

    @SuppressWarnings("unused")
    public String getSnippet() {
        return snippet;
    }

    @SuppressWarnings("unused")
    public String getDoes() {
        return does;
    }
}
