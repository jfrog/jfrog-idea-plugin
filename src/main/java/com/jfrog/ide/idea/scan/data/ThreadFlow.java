package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"locations"})
public class ThreadFlow {

    @JsonProperty("locations")
    private List<ThreadFlowLocation> locations = new ArrayList<>();

    @JsonProperty("locations")
    public List<ThreadFlowLocation> getLocations() {
        return locations;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setLocations(List<ThreadFlowLocation> locations) {
        this.locations = locations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ThreadFlow)) {
            return false;
        }
        ThreadFlow rhs = ((ThreadFlow) other);
        return (CollectionUtils.isEqualCollection(this.locations, rhs.locations));
    }

}
