package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "locations"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class ThreadFlow {

    @JsonProperty("locations")
    private List<ThreadFlowLocation> locations = new ArrayList<ThreadFlowLocation>();

    @JsonProperty("locations")
    public List<ThreadFlowLocation> getLocations() {
        return locations;
    }

    @JsonProperty("locations")
    public void setLocations(List<ThreadFlowLocation> locations) {
        this.locations = locations;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.locations == null) ? 0 : this.locations.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ThreadFlow) == false) {
            return false;
        }
        ThreadFlow rhs = ((ThreadFlow) other);
        return ((this.locations == rhs.locations) || ((this.locations != null) && this.locations.equals(rhs.locations)));
    }

}
