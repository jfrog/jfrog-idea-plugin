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
        "threadFlows"
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated("jsonschema2pojo")
public class CodeFlow {

    @JsonProperty("threadFlows")
    private List<ThreadFlow> threadFlows = new ArrayList<ThreadFlow>();

    @JsonProperty("threadFlows")
    public List<ThreadFlow> getThreadFlows() {
        return threadFlows;
    }

    @JsonProperty("threadFlows")
    public void setThreadFlows(List<ThreadFlow> threadFlows) {
        this.threadFlows = threadFlows;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result * 31) + ((this.threadFlows == null) ? 0 : this.threadFlows.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CodeFlow) == false) {
            return false;
        }
        CodeFlow rhs = ((CodeFlow) other);
        return ((this.threadFlows == rhs.threadFlows) || ((this.threadFlows != null) && this.threadFlows.equals(rhs.threadFlows)));
    }

}
