package org.jfrog.idea.xray;

import org.jfrog.idea.xray.persistency.types.GeneralInfo;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by romang on 3/9/17.
 */
public class ScanTreeNode extends DefaultMutableTreeNode {

    private Set<Issue> issues = new HashSet<>();
    private Set<License> licenses = new HashSet<>();
    private GeneralInfo generalInfo;

    public ScanTreeNode(Object userObject) {
        super(userObject);
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
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
     * @return all issues of the current node and it's ancestors
     */
    public Set<Issue> getAllIssues() {
        Set<Issue> allIssues = new HashSet<>();
        addIssuesRecursive(allIssues);
        return allIssues;
    }

    private void addIssuesRecursive(Set<Issue> issues) {
        if (!this.issues.isEmpty()) {
            issues.addAll(this.issues);
        }

        Enumeration c = children();
        while (c.hasMoreElements()) {
            ScanTreeNode node = (ScanTreeNode) c.nextElement();
            node.addIssuesRecursive(issues);
        }
    }

    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }
}
