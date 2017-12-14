package org.jfrog.idea.xray;

import org.jfrog.idea.xray.persistency.types.GeneralInfo;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by romang on 3/9/17.
 */
public class ScanTreeNode extends DefaultMutableTreeNode {

    private Set<Issue> issues = new HashSet<>();
    private Set<License> licenses = new HashSet<>();
    private GeneralInfo generalInfo;
    private Issue topIssue = new Issue();

    public ScanTreeNode(Object userObject) {
        super(userObject);
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
    }

    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    /**
     * @return current node's general info
     */
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    /**
     * @return current node's issues
     */
    public Set<Issue> getIssues() {
        return issues;
    }

    /**
     * @return current node's licenses
     */
    public Set<License> getLicenses() {
        return licenses;
    }

    /**
     * @return top severity issue of the current node and it's ancestors
     */
    public Issue getTopIssue() {
        return topIssue;
    }

    /**
     * @return total number of issues of the current node and it's ancestors
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * @return Node's children
     */
    public Vector<ScanTreeNode> getChildren() {
        return children != null ? children : new Vector<>();
    }

    /**
     * 1. Populate current node's issues components
     * 2. Populate current node and subtree's issues
     * 3. Populate current node and subtree's top issue
     * 4. Sort the tree
     * @return all issues of the current node and it's ancestors
     */
    public Set<Issue> processTreeIssues() {
        setIssuesComponent();
        getChildren().forEach(child -> issues.addAll(child.processTreeIssues()));
        setTopIssue();
        sortChildren();
        return issues;
    }

    private void setIssuesComponent() {
        issues.forEach(issue -> issue.setComponent(getUserObject().toString()));
    }

    private void sortChildren() {
        getChildren().sort(Comparator
                .comparing(ScanTreeNode::getTopIssue, Comparator.comparing(Issue::getSeverity))
                .thenComparing(ScanTreeNode::getIssueCount)
                .thenComparing(ScanTreeNode::getChildCount)
                .reversed());
    }

    private void setTopIssue() {
        issues.forEach(issue -> {
            if (topIssue.isTopSeverity()) {
                return;
            }
            if (issue.isHigherSeverityThan(topIssue)) {
                topIssue = issue;
            }
        });
    }
}
