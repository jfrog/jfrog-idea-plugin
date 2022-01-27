package com.jfrog.ide.idea.ui;

import com.jfrog.ide.idea.ui.components.ImpactPathPane;
import com.jfrog.ide.idea.ui.components.ReferencesPane;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import java.util.List;

/**
 * @author yahavi
 **/
public class IssueDetails extends MoreInfoPanel {
    public IssueDetails(Issue issue, DependencyTree impactedNode) {
        super();
        addText("Severity", issue.getSeverity().getSeverityName());
        String cves = CollectionUtils.isNotEmpty(issue.getCves()) ? String.join(", ", issue.getCves()) : "";
        addText("CVEs", cves);
        addText("Summary", issue.getSummary());
        List<String> fixedVersions = ListUtils.emptyIfNull(issue.getFixedVersions());
        addText("Fixed Versions", StringUtils.defaultIfEmpty(String.join(", ", fixedVersions), "[]"));
        addReferences(issue.getReferences());
        addImpactPath(impactedNode, issue.getSeverity());
    }

    /**
     * Add references to the Issue Details panel.
     *
     * @param list - The references list
     */
    private void addReferences(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        addComponent("References", new ReferencesPane(list));
    }

    /**
     * Add impact path graph to the Issue Details panel.
     *
     * @param impactedNode - The impacted node
     * @param severity     - Issue severity
     */
    private void addImpactPath(DependencyTree impactedNode, Severity severity) {
        addComponent("Impact path", new ImpactPathPane(impactedNode, severity));
    }

}
