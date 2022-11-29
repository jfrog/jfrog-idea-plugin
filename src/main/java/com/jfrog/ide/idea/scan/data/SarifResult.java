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
    "message",
    "locations",
    "ruleId",
    "codeFlows"
})
@Generated("jsonschema2pojo")
@JsonIgnoreProperties
public class SarifResult {

    @JsonProperty("message")
    private Message message;
    @JsonProperty("locations")
    private List<Location> locations = new ArrayList<Location>();
    @JsonProperty("ruleId")
    private String ruleId;
    @JsonProperty("codeFlows")
    private List<CodeFlow> codeFlows = new ArrayList<CodeFlow>();

    @JsonProperty("message")
    public Message getMessage() {
        return message;
    }

    @JsonProperty("message")
    public void setMessage(Message message) {
        this.message = message;
    }

    @JsonProperty("locations")
    public List<Location> getLocations() {
        return locations;
    }

    @JsonProperty("locations")
    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    @JsonProperty("ruleId")
    public String getRuleId() {
        return ruleId;
    }

    @JsonProperty("ruleId")
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    @JsonProperty("codeFlows")
    public List<CodeFlow> getCodeFlows() {
        return codeFlows;
    }

    @JsonProperty("codeFlows")
    public void setCodeFlows(List<CodeFlow> codeFlows) {
        this.codeFlows = codeFlows;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SarifResult.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("locations");
        sb.append('=');
        sb.append(((this.locations == null)?"<null>":this.locations));
        sb.append(',');
        sb.append("ruleId");
        sb.append('=');
        sb.append(((this.ruleId == null)?"<null>":this.ruleId));
        sb.append(',');
        sb.append("codeFlows");
        sb.append('=');
        sb.append(((this.codeFlows == null)?"<null>":this.codeFlows));
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
        result = ((result* 31)+((this.locations == null)? 0 :this.locations.hashCode()));
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.ruleId == null)? 0 :this.ruleId.hashCode()));
        result = ((result* 31)+((this.codeFlows == null)? 0 :this.codeFlows.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SarifResult) == false) {
            return false;
        }
        SarifResult rhs = ((SarifResult) other);
        return (((((this.locations == rhs.locations)||((this.locations!= null)&&this.locations.equals(rhs.locations)))&&((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message))))&&((this.ruleId == rhs.ruleId)||((this.ruleId!= null)&&this.ruleId.equals(rhs.ruleId))))&&((this.codeFlows == rhs.codeFlows)||((this.codeFlows!= null)&&this.codeFlows.equals(rhs.codeFlows))));
    }

}
