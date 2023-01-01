package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeFlow {

    @JsonProperty("threadFlows")
    private List<ThreadFlow> threadFlows = new ArrayList<>();

    @SuppressWarnings("unused")
    public List<ThreadFlow> getThreadFlows() {
        return threadFlows;
    }

    @SuppressWarnings("unused")
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
        return (CollectionUtils.isEqualCollection(this.threadFlows, rhs.threadFlows));
    }

}
