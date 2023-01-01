package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({"executionSuccessful", "arguments", "workingDirectory"})
public class Invocation {

    @JsonProperty("executionSuccessful")
    private Boolean executionSuccessful;
    @JsonProperty("arguments")
    private List<String> arguments = new ArrayList<>();
    @JsonProperty("workingDirectory")
    private WorkingDirectory workingDirectory;

    @SuppressWarnings("unused")
    public Boolean getExecutionSuccessful() {
        return executionSuccessful;
    }

    @SuppressWarnings("unused")
    public void setExecutionSuccessful(Boolean executionSuccessful) {
        this.executionSuccessful = executionSuccessful;
    }

    @SuppressWarnings("unused")
    public List<String> getArguments() {
        return arguments;
    }

    @SuppressWarnings("unused")
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    @SuppressWarnings("unused")
    public WorkingDirectory getWorkingDirectory() {
        return workingDirectory;
    }

    @SuppressWarnings("unused")
    public void setWorkingDirectory(WorkingDirectory workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arguments, executionSuccessful, workingDirectory);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Invocation)) {
            return false;
        }
        Invocation rhs = ((Invocation) other);
        return (((CollectionUtils.isEqualCollection(this.arguments, rhs.arguments)) && (Objects.equals(this.executionSuccessful, rhs.executionSuccessful))) && (Objects.equals(this.workingDirectory, rhs.workingDirectory)));
    }

}
