package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"location"})
public class ThreadFlowLocation {

    @JsonProperty("location")
    private Location location;

    @SuppressWarnings("UnusedReturnValue")

    public Location getLocation() {
        return location;
    }

    @SuppressWarnings("UnusedReturnValue")

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ThreadFlowLocation)) {
            return false;
        }
        ThreadFlowLocation rhs = ((ThreadFlowLocation) other);
        return (Objects.equals(this.location, rhs.location));
    }

}
