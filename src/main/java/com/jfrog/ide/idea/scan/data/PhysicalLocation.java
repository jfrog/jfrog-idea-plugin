package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"artifactLocation", "region"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class PhysicalLocation {

    @JsonProperty("artifactLocation")
    private ArtifactLocation artifactLocation;
    @JsonProperty("region")
    private Region region;

    @JsonProperty("artifactLocation")
    public ArtifactLocation getArtifactLocation() {
        return artifactLocation;
    }

    @JsonProperty("artifactLocation")
    public void setArtifactLocation(ArtifactLocation artifactLocation) {
        this.artifactLocation = artifactLocation;
    }

    @JsonProperty("region")
    public Region getRegion() {
        return region;
    }

    @JsonProperty("region")
    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.region == null) ? 0 : this.region.hashCode()));
        result = ((result * 31) + ((this.artifactLocation == null) ? 0 : this.artifactLocation.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PhysicalLocation) == false) {
            return false;
        }
        PhysicalLocation rhs = ((PhysicalLocation) other);
        return (((this.region == rhs.region) || ((this.region != null) && this.region.equals(rhs.region))) && ((this.artifactLocation == rhs.artifactLocation) || ((this.artifactLocation != null) && this.artifactLocation.equals(rhs.artifactLocation))));
    }

}
