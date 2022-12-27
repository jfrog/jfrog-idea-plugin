package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"uri"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class WorkingDirectory {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("uri")
    public String getUri() {
        return uri;
    }

    @JsonProperty("uri")
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.uri == null) ? 0 : this.uri.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof WorkingDirectory) == false) {
            return false;
        }
        WorkingDirectory rhs = ((WorkingDirectory) other);
        return ((this.uri == rhs.uri) || ((this.uri != null) && this.uri.equals(rhs.uri)));
    }

}
