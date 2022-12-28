package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "runs",
        "version",
        "$schema"
})
public class Output {

    @JsonProperty("runs")
    private List<Run> runs = new ArrayList<>();
    @JsonProperty("version")
    private String version;

    public List<Run> getRuns() {
        return runs;
    }
    @SuppressWarnings("UnusedReturnValue")
    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(runs, version);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Output)) {
            return false;
        }
        Output rhs = ((Output) other);
        return ((CollectionUtils.isEqualCollection(this.runs, rhs.runs))) && (Objects.equals(this.version, rhs.version));
    }

}
