package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "artifactLocation",
    "region"
})
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PhysicalLocation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("artifactLocation");
        sb.append('=');
        sb.append(((this.artifactLocation == null)?"<null>":this.artifactLocation));
        sb.append(',');
        sb.append("region");
        sb.append('=');
        sb.append(((this.region == null)?"<null>":this.region));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.region == null)? 0 :this.region.hashCode()));
        result = ((result* 31)+((this.artifactLocation == null)? 0 :this.artifactLocation.hashCode()));
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
        return (((this.region == rhs.region)||((this.region!= null)&&this.region.equals(rhs.region)))&&((this.artifactLocation == rhs.artifactLocation)||((this.artifactLocation!= null)&&this.artifactLocation.equals(rhs.artifactLocation))));
    }

}
