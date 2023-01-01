package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"id", "shortDescription"})
public class Rule {

    @JsonProperty("id")
    private String id;

    @JsonProperty("shortDescription")
    private Message shortDescription;

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

    @Override
    public int hashCode() {
        return Objects.hash(id, shortDescription);
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
        return (Objects.equals(this.id, rhs.id) && Objects.equals(this.shortDescription, rhs.shortDescription));
    }

}
