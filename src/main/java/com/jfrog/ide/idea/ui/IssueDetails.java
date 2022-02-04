package com.jfrog.ide.idea.ui;

import com.jfrog.ide.idea.ui.components.ImpactPathPane;
import com.jfrog.ide.idea.ui.components.ReferencesPane;
import org.apache.commons.collections4.CollectionUtils;
import org.jfrog.build.extractor.scan.Cve;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.Issue;
import org.jfrog.build.extractor.scan.Severity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class IssueDetails extends MoreInfoPanel {
    public IssueDetails(Issue issue, DependencyTree impactedNode) {
        super();
        addText("Severity", issue.getSeverity().getSeverityName());
        addText("CVEs", getCves(issue));
        addText("Summary", issue.getSummary());
        addReferences(issue.getReferences());
        addImpactPath(impactedNode, issue.getSeverity());
    }

    /**
     * Create the CVEs string seperated by ",".
     *
     * @param issue - The issue containing the CVEs.
     * @return the CVEs string.
     */
    private String getCves(Issue issue) {
        List<Cve> cves = issue.getCves();
        if (CollectionUtils.isEmpty(cves)) {
            return "";
        }
        return cves.stream().map(Cve::getCveId).collect(Collectors.joining(" ,"));
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
