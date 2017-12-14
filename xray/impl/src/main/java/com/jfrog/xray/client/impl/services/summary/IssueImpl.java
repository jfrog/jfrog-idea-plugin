package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.Issue;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IssueImpl implements Issue {

    private static final long serialVersionUID = 1L;

    private String summary;
    private String description;
    private String severity;
    private String provider;
    private String created;
    @JsonProperty("issue_type")
    private String issueType;
    @JsonProperty("impact_path")
    private List<String> impactPath = null;

    @Override
    @JsonProperty("summary")
    public String getSummary() {
        return summary;
    }

    @Override
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @Override
    @JsonProperty("issue_type")
    public String getIssueType() {
        return issueType;
    }

    @Override
    @JsonProperty("severity")
    public String getSeverity() {
        return severity;
    }

    @Override
    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    @Override
    @JsonProperty("created")
    public String getCreated() {
        return created;
    }

    @Override
    @JsonProperty("impact_path")
    public List<String> getImpactPath() {
        return impactPath;
    }

    @Override
    public boolean equals(Object obj) {
        IssueImpl issue = (IssueImpl) obj;
        return issue.summary.equals(summary) && issue.description.equals(description);
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + summary.hashCode();
        return result;
    }
}