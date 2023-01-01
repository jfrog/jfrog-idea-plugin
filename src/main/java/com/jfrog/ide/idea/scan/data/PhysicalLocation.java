package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
public class PhysicalLocation {

    @JsonProperty("artifactLocation")
    private ArtifactLocation artifactLocation;
    @JsonProperty("region")
    private Region region;

    public ArtifactLocation getArtifactLocation() {
        return artifactLocation;
    }

    @SuppressWarnings("unused")
    public void setArtifactLocation(ArtifactLocation artifactLocation) {
        this.artifactLocation = artifactLocation;
    }

    public Region getRegion() {
        return region;
    }

    @SuppressWarnings("unused")
    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, artifactLocation);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof PhysicalLocation)) {
            return false;
        }
        PhysicalLocation rhs = ((PhysicalLocation) other);
        return ((Objects.equals(this.region, rhs.region)) && (Objects.equals(this.artifactLocation, rhs.artifactLocation)));
    }

}
