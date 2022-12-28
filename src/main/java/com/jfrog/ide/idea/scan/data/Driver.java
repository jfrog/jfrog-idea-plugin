package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({
        "name"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Driver {

    @JsonProperty("name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Driver)) {
            return false;
        }
        Driver rhs = ((Driver) other);
        return (Objects.equals(this.name, rhs.name));
    }

}
