package org.jfrog.idea.xray;

import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.persistency.types.Severity;

import java.util.UUID;

/**
 * Created by Yahav Itzhak on 25 Dec 2017.
 */
public class ScanTreeNodeBase {

    ScanTreeNode root, one, two, three, four, five;

    /**
     * Build an empty tree with 5 nodes
     */
    public void init() {
        root = new ScanTreeNode("0");
        one = new ScanTreeNode("1");
        two = new ScanTreeNode("2");
        three = new ScanTreeNode("3");
        four = new ScanTreeNode("4");
        five = new ScanTreeNode("5");
        root.add(one); // 0 -> 1
        root.add(two); // 0 -> 2
        two.add(three); // 2 -> 3
        two.add(four); // 2 -> 4
        four.add(five); // 4 -> 5
    }

    /**
     * Create a random issue
     * @param severity the issue severity
     * @return the random issue
     */
    Issue createIssue(Severity severity) {
        Issue issue = new Issue();
        issue.severity = severity;
        issue.description = UUID.randomUUID().toString();
        issue.issueType = UUID.randomUUID().toString();
        issue.created = UUID.randomUUID().toString();
        issue.provider = UUID.randomUUID().toString();
        issue.summary = UUID.randomUUID().toString();
        return issue;
    }

    /**
     * Create a license
     * @param name the license name
     * @return the license
     */
    License createLicense(String name) {
        License license = new License();
        license.name = name;
        license.fullName = name;
        return license;
    }
}
