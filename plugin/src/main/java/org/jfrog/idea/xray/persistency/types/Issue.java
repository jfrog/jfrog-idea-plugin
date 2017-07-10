package org.jfrog.idea.xray.persistency.types;

import org.jetbrains.annotations.NotNull;

/**
 * Created by romang on 4/12/17.
 */

public class Issue implements Comparable<Issue> {

    public String created;
    public String description;
    public String issueType;
    public String provider;
    public String sevirity;
    public String summary;

    public Issue() {
    }

    public Issue(com.jfrog.xray.client.services.summary.Issue issue) {
        created = issue.getCreated();
        description = issue.getDescription();
        issueType = issue.getIssueType();
        provider = issue.getProvider();
        sevirity = issue.getSeverity();
        summary = issue.getSummary();
    }

    public Severity getSeverity() {
        return Severity.valueOf(sevirity.toLowerCase());
    }

    @Override
    public int compareTo(@NotNull Issue o) {
        return Integer.compare(getSeverity().getValue(), o.getSeverity().getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Issue issue = (Issue) o;

        if (!description.equals(issue.description)) return false;
        return summary.equals(issue.summary);
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + summary.hashCode();
        return result;
    }
}
