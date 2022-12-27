package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.annotation.processing.Generated;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"tool", "invocations", "results"})
@Generated("jsonschema2pojo")
public class Run {

    @JsonProperty("tool")
    private Tool tool;
    @JsonProperty("invocations")
    private List<Invocation> invocations = new ArrayList<Invocation>();
    @JsonProperty("results")
    private List<SarifResult> results = new ArrayList<SarifResult>();

    @JsonProperty("tool")
    public Tool getTool() {
        return tool;
    }

    @JsonProperty("tool")
    public void setTool(Tool tool) {
        this.tool = tool;
    }

    @JsonProperty("invocations")
    public List<Invocation> getInvocations() {
        return invocations;
    }

    @JsonProperty("invocations")
    public void setInvocations(List<Invocation> invocations) {
        this.invocations = invocations;
    }

    @JsonProperty("results")
    public List<SarifResult> getResults() {
        return results;
    }

    @JsonProperty("results")
    public void setResults(List<SarifResult> results) {
        this.results = results;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.results == null) ? 0 : this.results.hashCode()));
        result = ((result * 31) + ((this.tool == null) ? 0 : this.tool.hashCode()));
        result = ((result * 31) + ((this.invocations == null) ? 0 : this.invocations.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Run) == false) {
            return false;
        }
        Run rhs = ((Run) other);
        return ((((this.results == rhs.results) || ((this.results != null) && this.results.equals(rhs.results))) && ((this.tool == rhs.tool) || ((this.tool != null) && this.tool.equals(rhs.tool)))) && ((this.invocations == rhs.invocations) || ((this.invocations != null) && this.invocations.equals(rhs.invocations))));
    }

}
