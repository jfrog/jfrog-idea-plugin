package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties
@JsonPropertyOrder({
    "runs",
    "version",
    "$schema"
})
@Generated("jsonschema2pojo")
public class Output {

    @JsonProperty("runs")
    private List<Run> runs = new ArrayList<Run>();
    @JsonProperty("version")
    private String version;
    @JsonProperty("$schema")
    private String $schema;

    @JsonProperty("runs")
    public List<Run> getRuns() {
        return runs;
    }

    @JsonProperty("runs")
    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("version")
    public void setVersion(String version) {
        this.version = version;
    }

    @JsonProperty("$schema")
    public String get$schema() {
        return $schema;
    }

    @JsonProperty("$schema")
    public void set$schema(String $schema) {
        this.$schema = $schema;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.$schema == null)? 0 :this.$schema.hashCode()));
        result = ((result* 31)+((this.runs == null)? 0 :this.runs.hashCode()));
        result = ((result* 31)+((this.version == null)? 0 :this.version.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Output) == false) {
            return false;
        }
        Output rhs = ((Output) other);
        return ((((this.$schema == rhs.$schema)||((this.$schema!= null)&&this.$schema.equals(rhs.$schema)))&&((this.runs == rhs.runs)||((this.runs!= null)&&this.runs.equals(rhs.runs))))&&((this.version == rhs.version)||((this.version!= null)&&this.version.equals(rhs.version))));
    }

}
