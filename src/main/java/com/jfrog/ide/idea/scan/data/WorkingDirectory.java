package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class WorkingDirectory {

    @JsonProperty("uri")
    private String uri;

    @SuppressWarnings("unused")
    public String getUri() {
        return uri;
    }

    @SuppressWarnings("unused")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof WorkingDirectory)) {
            return false;
        }
        WorkingDirectory rhs = ((WorkingDirectory) other);
        return (Objects.equals(this.uri, rhs.uri));
    }
}
