package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class Location implements Serializable {
    private String file;
    private String fileName;
    private String snippet;
    private int startRow;
    private int startColumn;
    private int endRow;
    private int endColumn;

    public Location() {
        this.file = "";
        this.fileName = "";
        this.snippet = "";
        this.startRow = 0;
        this.startColumn = 0;
        this.endRow = 0;
        this.endColumn = 0;
    }

    public Location(String file, String fileName, int startRow, int startColumn, int endRow, int endColumn, String snippet) {
        this.file = file;
        this.fileName = fileName;
        this.snippet = snippet;
        this.startRow = startRow;
        this.startColumn = startColumn;
        this.endRow = endRow;
        this.endColumn = endColumn;
    }

    public Location(Location other) {
        this(other.file, other.fileName, other.startRow, other.startColumn, other.endRow, other.endColumn, other.snippet);
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
    public String getSnippet() {
        return snippet;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndRow() {
        return endRow;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public void setStartColumn(int startColumn) {
        this.startColumn = startColumn;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }
}
