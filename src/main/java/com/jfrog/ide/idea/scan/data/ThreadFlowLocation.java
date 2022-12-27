package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"location"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class ThreadFlowLocation {

    @JsonProperty("location")
    private Location location;

    @JsonProperty("location")
    public Location getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.location == null) ? 0 : this.location.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ThreadFlowLocation) == false) {
            return false;
        }
        ThreadFlowLocation rhs = ((ThreadFlowLocation) other);
        return ((this.location == rhs.location) || ((this.location != null) && this.location.equals(rhs.location)));
    }

}
