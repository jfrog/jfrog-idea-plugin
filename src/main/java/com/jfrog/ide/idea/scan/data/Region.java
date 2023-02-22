package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Region {

    @JsonProperty("endColumn")
    private int endColumn;
    @JsonProperty("endLine")
    private int endLine;
    @JsonProperty("startColumn")
    private int startColumn;
    @JsonProperty("startLine")
    private int startLine;
    @JsonProperty("snippet")

    private Message snippet;

    public int getEndColumn() {
        return endColumn;
    }

    @SuppressWarnings("unused")
    public void setEndColumn(int endColumn) {
        this.endColumn = endColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    @SuppressWarnings("unused")
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    @SuppressWarnings("unused")
    public void setStartColumn(int startColumn) {
        this.startColumn = startColumn;
    }

    public int getStartLine() {
        return startLine;
    }

    @SuppressWarnings("unused")
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public Message getSnippet() {
        return snippet;
    }

    @SuppressWarnings("unused")
    public void setSnippet(Message snippet) {
        this.snippet = snippet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(endLine, endColumn, startColumn, startLine, snippet);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Region)) {
            return false;
        }
        Region rhs = ((Region) other);
        return ((((Objects.equals(this.endLine, rhs.endLine)) && (Objects.equals(this.endColumn, rhs.endColumn))) && (Objects.equals(this.startColumn, rhs.startColumn))) && (Objects.equals(this.startLine, rhs.startLine)) && (Objects.equals(this.snippet, rhs.snippet)));
    }

}
