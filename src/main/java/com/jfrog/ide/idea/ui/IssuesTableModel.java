package com.jfrog.ide.idea.ui;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.extractor.scan.Issue;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Yahav Itzhak on 13 Nov 2017.
 */
public class IssuesTableModel extends AbstractTableModel {

    private final Set<Issue> issues;
    private final Set<String> components;

    IssuesTableModel() {
        this(Sets.newHashSet(), Sets.newHashSet());
    }

    IssuesTableModel(@NotNull Set<Issue> issues, @NotNull Set<String> components) {
        this.issues = issues.stream()
                .filter(issue -> StringUtils.isNotBlank(issue.getSummary()))
                .collect(Collectors.toSet());
        this.components = components;
    }

    public enum IssueColumn {
        SEVERITY("Severity"),
        COMPONENT("Impacted Component"),
        FIXED_VERSIONS("Fixed Versions");

        private final String name;

        IssueColumn(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public Set<String> getComponents() {
        return this.components;
    }

    @Override
    public int getColumnCount() {
        return IssueColumn.values().length;
    }

    @Override
    public int getRowCount() {
        return issues.size();
    }

    @Override
    public String getColumnName(int col) {
        if (col == IssueColumn.SEVERITY.ordinal()) {
            return "";
        }
        return IssueColumn.values()[col].getName();
    }

    @Override
    public Object getValueAt(int row, int col) {
        IssueColumn issueColumn = IssueColumn.valueOf(IssueColumn.values()[col].toString());
        Issue issue = getIssueAt(row);
        switch (issueColumn) {
            case SEVERITY:
                return issue.getSeverity();
            case COMPONENT:
                return issue.getComponent();
            case FIXED_VERSIONS:
                List<String> fixedVersions = issue.getFixedVersions() == null ? Collections.emptyList() : issue.getFixedVersions();
                return StringUtils.defaultIfEmpty(String.join(", ", fixedVersions), "[]");
        }
        return "N/A";
    }

    /**
     * Get the issue at the input row.
     *
     * @param row - The row number
     * @return the issue of the input row.
     */
    Issue getIssueAt(int row) {
        return (Issue) issues.toArray()[row];
    }
}