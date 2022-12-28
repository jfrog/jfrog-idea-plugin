package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "threadFlows"
})
public class CodeFlow {

    @JsonProperty("threadFlows")
    private List<ThreadFlow> threadFlows = new ArrayList<>();

    @SuppressWarnings("UnusedReturnValue")
    public List<ThreadFlow> getThreadFlows() {
        return threadFlows;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setThreadFlows(List<ThreadFlow> threadFlows) {
        this.threadFlows = threadFlows;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadFlows);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof CodeFlow)) {
            return false;
        }
        CodeFlow rhs = ((CodeFlow) other);
        return (Objects.equals(this.threadFlows, rhs.threadFlows));
    }

}
