package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "message",
        "locations",
        "ruleId",
        "codeFlows"
})
public class SarifResult {

    @JsonProperty("message")
    private Message message;
    @JsonProperty("locations")
    private List<Location> locations = new ArrayList<>();
    @JsonProperty("ruleId")
    private String ruleId;
    @JsonProperty("codeFlows")
    private List<CodeFlow> codeFlows = new ArrayList<>();

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public List<CodeFlow> getCodeFlows() {
        return codeFlows;
    }

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

}
