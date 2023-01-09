package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Rule {

    @JsonProperty("id")
    private String id;

    @JsonProperty("shortDescription")
    private Message shortDescription;

    @JsonProperty("fullDescription")
    private Message fullDescription;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @SuppressWarnings({"unused"})
    public Message getShortDescription() {
        return shortDescription;
    }

    @SuppressWarnings({"unused"})
    public void setShortDescription(Message shortDescription) {
        this.shortDescription = shortDescription;
    }

    @SuppressWarnings({"unused"})
    public Message getFullDescription() {
        return fullDescription;
    }

    @SuppressWarnings({"unused"})
    public void setFullDescription(Message fullDescription) {
        this.fullDescription = fullDescription;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Rule)) {
            return false;
        }
        Rule rhs = ((Rule) other);
        return Objects.equals(this.id, rhs.id);
    }
}
