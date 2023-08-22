package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class SarifResult {
    @JsonProperty("message")
    private Message message;
    @JsonProperty("locations")
    private List<Location> locations = new ArrayList<>();
    @JsonProperty("ruleId")
    private String ruleId;
    @JsonProperty("codeFlows")
    private List<CodeFlow> codeFlows = new ArrayList<>();
    @JsonProperty("kind")
    private String kind;
    @JsonProperty("level")
    private String severity;
    @JsonProperty("suppressions")
    private AnalyzeSuppression[] suppressions;

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setSuppressions(AnalyzeSuppression[] suppressions) {
        this.suppressions = suppressions;
    }

    public String getKind() {
        return StringUtils.defaultString(kind);
    }

    public String getSeverity() {
        return StringUtils.defaultString(severity, "warning");
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    @SuppressWarnings({"unused"})
    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    @SuppressWarnings({"unused"})
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @SuppressWarnings({"unused"})
    public List<CodeFlow> getCodeFlows() {
        return codeFlows;
    }

    @SuppressWarnings({"unused"})
    public void setCodeFlows(List<CodeFlow> codeFlows) {
        this.codeFlows = codeFlows;
    }

    @Override
    public int hashCode() {
        return Objects.hash(locations, message, ruleId, codeFlows);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SarifResult)) {
            return false;
        }
        SarifResult rhs = ((SarifResult) other);
        return ((((CollectionUtils.isEqualCollection(this.locations, rhs.locations)) && (Objects.equals(this.message, rhs.message))) && (Objects.equals(this.ruleId, rhs.ruleId))) && (CollectionUtils.isEqualCollection(this.codeFlows, rhs.codeFlows)));
    }

    public boolean isNotSuppressed() {
        return ArrayUtils.isEmpty(suppressions);
    }
}
