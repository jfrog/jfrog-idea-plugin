package com.jfrog.ide.idea.ui.webview.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {
    private final String file;
    private final String fileName;
    // For webview version 0.1.22
    @JsonProperty("row")
    private final int startRow;
    private final int startColumn;
    private final int endRow;
    private final int endColumn;
    private final String snippet;

    public Location(String file, String fileName, int startRow, int startColumn, int endRow, int endColumn, String snippet) {
        this.file = file;
        this.fileName = fileName;
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.endRow = endRow;
        this.endColumn = endColumn;
        this.snippet = snippet;
    }

    @SuppressWarnings("unused")
    public String getFile() {
        return file;
    }

    @SuppressWarnings("unused")
    public String getFileName() {
        return fileName;
    }

    @SuppressWarnings("unused")
    public int getStartRow() {
        return startRow;
    }

    @SuppressWarnings("unused")
    public int getStartColumn() {
        return startColumn;
    }

    @SuppressWarnings("unused")
    public int getEndRow() {
        return endRow;
    }

    @SuppressWarnings("unused")
    public int getEndColumn() {
        return endColumn;
    }

    @SuppressWarnings("unused")
    public String getSnippet() {
        return snippet;
    }
}
