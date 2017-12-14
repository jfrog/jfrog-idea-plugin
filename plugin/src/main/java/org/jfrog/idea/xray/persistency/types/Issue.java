package org.jfrog.idea.xray.persistency.types;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by romang on 4/12/17.
 */
public class Issue implements Comparable<Issue> {

    public String created;
    public String description;
    public String issueType = "N/A";
    public String provider;
    @Deprecated
    public String sevirity;
    public Severity severity = Severity.Normal;
    public String summary;
    public String component = "";

    public Issue() {
    }

    public Issue(com.jfrog.xray.client.services.summary.Issue issue) {
        created = issue.getCreated();
        description = issue.getDescription();
        issueType = issue.getIssueType();
        provider = issue.getProvider();
        severity = Severity.valueOf(issue.getSeverity());
        summary = issue.getSummary();
    }

    public Severity getSeverity() {
        if (this.sevirity != null) {
            this.severity = Severity.valueOf(sevirity);
            this.sevirity = null;
        }
        return this.severity;
    }

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public boolean isTopSeverity() {
        return getSeverity() == Severity.Critical;
    }

    public boolean isHigherSeverityThan(@NotNull Issue o) {
        return getSeverity().compareTo(o.getSeverity()) > 0;
    }

    @Override
    public int compareTo(@NotNull Issue issue) {
        return Integer.compare(hashCode(), issue.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        Issue issue = (Issue) o;
        String component = this.component != null ? this.component : "";
        String summary = this.summary != null ? this.summary : "";
        String description = this.description != null ? this.description : "";
        if (StringUtils.isEmpty(component)) {
            return description.equals(issue.description) && summary.equals(issue.summary);
        }
        return component.equals(issue.component) && summary.equals(issue.summary);
    }

    @Override
    public int hashCode() {
        String component = this.component != null ? this.component : "";
        String summary = this.summary != null ? this.summary : "";
        String description = this.description != null ? this.description : "";
        int result = summary.hashCode();
        result += StringUtils.isEmpty(component) ? description.hashCode() : component.hashCode();
        return result * 31;
    }
}