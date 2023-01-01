package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Region {

    @JsonProperty("endColumn")
    private Integer endColumn;
    @JsonProperty("endLine")
    private Integer endLine;
    @JsonProperty("startColumn")
    private Integer startColumn;
    @JsonProperty("startLine")
    private Integer startLine;
    @JsonProperty("snippet")

    private Message snippet;

    public Integer getEndColumn() {
        return endColumn;
    }

    @SuppressWarnings("unused")
    public void setEndColumn(Integer endColumn) {
        this.endColumn = endColumn;
    }

    public Integer getEndLine() {
        return endLine;
    }

    @SuppressWarnings("unused")
    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public Integer getStartColumn() {
        return startColumn;
    }

    @SuppressWarnings("unused")
    public void setStartColumn(Integer startColumn) {
        this.startColumn = startColumn;
    }

    public Integer getStartLine() {
        return startLine;
    }

    @SuppressWarnings("unused")
    public void setStartLine(Integer startLine) {
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
