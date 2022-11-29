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
    "executionSuccessful",
    "arguments",
    "workingDirectory"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class Invocation {

    @JsonProperty("executionSuccessful")
    private Boolean executionSuccessful;
    @JsonProperty("arguments")
    private List<String> arguments = new ArrayList<String>();
    @JsonProperty("workingDirectory")
    private WorkingDirectory workingDirectory;

    @JsonProperty("executionSuccessful")
    public Boolean getExecutionSuccessful() {
        return executionSuccessful;
    }

    @JsonProperty("executionSuccessful")
    public void setExecutionSuccessful(Boolean executionSuccessful) {
        this.executionSuccessful = executionSuccessful;
    }

    @JsonProperty("arguments")
    public List<String> getArguments() {
        return arguments;
    }

    @JsonProperty("arguments")
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    @JsonProperty("workingDirectory")
    public WorkingDirectory getWorkingDirectory() {
        return workingDirectory;
    }

    @JsonProperty("workingDirectory")
    public void setWorkingDirectory(WorkingDirectory workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Invocation.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("executionSuccessful");
        sb.append('=');
        sb.append(((this.executionSuccessful == null)?"<null>":this.executionSuccessful));
        sb.append(',');
        sb.append("arguments");
        sb.append('=');
        sb.append(((this.arguments == null)?"<null>":this.arguments));
        sb.append(',');
        sb.append("workingDirectory");
        sb.append('=');
        sb.append(((this.workingDirectory == null)?"<null>":this.workingDirectory));
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
        result = ((result* 31)+((this.arguments == null)? 0 :this.arguments.hashCode()));
        result = ((result* 31)+((this.executionSuccessful == null)? 0 :this.executionSuccessful.hashCode()));
        result = ((result* 31)+((this.workingDirectory == null)? 0 :this.workingDirectory.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Invocation) == false) {
            return false;
        }
        Invocation rhs = ((Invocation) other);
        return ((((this.arguments == rhs.arguments)||((this.arguments!= null)&&this.arguments.equals(rhs.arguments)))&&((this.executionSuccessful == rhs.executionSuccessful)||((this.executionSuccessful!= null)&&this.executionSuccessful.equals(rhs.executionSuccessful))))&&((this.workingDirectory == rhs.workingDirectory)||((this.workingDirectory!= null)&&this.workingDirectory.equals(rhs.workingDirectory))));
    }

}
