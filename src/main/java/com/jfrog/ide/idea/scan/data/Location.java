package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "physicalLocation"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class Location {

    @JsonProperty("physicalLocation")
    private PhysicalLocation physicalLocation;

    @JsonProperty("physicalLocation")
    public PhysicalLocation getPhysicalLocation() {
        return physicalLocation;
    }

    @JsonProperty("physicalLocation")
    public void setPhysicalLocation(PhysicalLocation physicalLocation) {
        this.physicalLocation = physicalLocation;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.physicalLocation == null)? 0 :this.physicalLocation.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Location) == false) {
            return false;
        }
        Location rhs = ((Location) other);
        return ((this.physicalLocation == rhs.physicalLocation)||((this.physicalLocation!= null)&&this.physicalLocation.equals(rhs.physicalLocation)));
    }

}
