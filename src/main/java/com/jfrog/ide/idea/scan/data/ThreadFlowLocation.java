package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ThreadFlowLocation {

    @JsonProperty("location")
    private Location location;

    @SuppressWarnings("unused")
    public Location getLocation() {
        return location;
    }

    @SuppressWarnings("unused")
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
