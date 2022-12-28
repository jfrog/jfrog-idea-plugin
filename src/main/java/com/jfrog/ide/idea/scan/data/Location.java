package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({
        "physicalLocation"
})
public class Location {

    @JsonProperty("physicalLocation")
    private PhysicalLocation physicalLocation;

    public PhysicalLocation getPhysicalLocation() {
        return physicalLocation;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setPhysicalLocation(PhysicalLocation physicalLocation) {
        this.physicalLocation = physicalLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalLocation);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Location)) {
            return false;
        }
        Location rhs = ((Location) other);
        return (Objects.equals(this.physicalLocation, rhs.physicalLocation));
    }

}
