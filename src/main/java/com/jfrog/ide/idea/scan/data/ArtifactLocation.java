package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({
        "uri"
})
public class ArtifactLocation {

    @JsonProperty("uri")
    private String uri;

    public String getUri() {
        return uri;
    }

    @SuppressWarnings("UnusedReturnValue")
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
        if (!(other instanceof ArtifactLocation)) {
            return false;
        }
        ArtifactLocation rhs = ((ArtifactLocation) other);
        return (Objects.equals(this.uri, rhs.uri));
    }
}
