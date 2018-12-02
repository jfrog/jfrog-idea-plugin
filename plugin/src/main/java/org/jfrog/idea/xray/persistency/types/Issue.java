package org.jfrog.idea.xray.persistency.types;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Created by romang on 4/12/17.
 */
public class Issue implements Comparable<Issue> {

    public String created;
    public String description;
    public String issueType = "N/A";
    public String provider;
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
        severity = Severity.fromString(issue.getSeverity());
        summary = issue.getSummary();
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public boolean isTopSeverity() {
        return getSeverity() == Severity.High;
    }

    public boolean isHigherSeverityThan(@NotNull Issue o) {
        return getSeverity().isHigherThan(o.getSeverity());
    }

    @Override
    public int compareTo(@NotNull Issue otherIssue) {
        return Integer.compare(hashCode(), Objects.hashCode(otherIssue));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Issue otherIssue = (Issue) other;
        if (StringUtils.isEmpty(component)) {
            return StringUtils.equals(description, otherIssue.description) && StringUtils.equals(summary, otherIssue.summary);
        }
        return StringUtils.equals(component, otherIssue.component) && StringUtils.equals(summary, otherIssue.summary);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(summary);
        result += StringUtils.isEmpty(component) ? Objects.hashCode(description) : Objects.hashCode(component);
        return result * 31;
    }
}