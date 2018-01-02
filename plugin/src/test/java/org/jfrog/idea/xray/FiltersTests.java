package org.jfrog.idea.xray;

import com.google.common.collect.Sets;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.persistency.types.Severity;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Yahav Itzhak on 25 Dec 2017.
 */
public class FiltersTests extends ScanTreeNodeBase {

    private FilterManager filterManager = new FilterManager();
    private Map<Severity, Boolean> severitiesFilters;
    private Map<License, Boolean> licensesFilters;

    /**
     * Init the FilterManager to accept all severities and MIT license.
     */
    @BeforeTest
    public void init() {
        super.init();
        filterManager = new FilterManager();
        severitiesFilters = filterManager.selectedSeverities;
        for (Severity severity : Severity.values()) {
            severitiesFilters.put(severity, true);
        }
        licensesFilters = filterManager.selectedLicenses;
        licensesFilters.put(createLicense("MIT"), true);
    }

    @Test
    public void testNoFilter() {
        ScanTreeNode issuesFilteredRoot = new ScanTreeNode("0");
        ScanTreeNode licenseFilteredRoot = new ScanTreeNode("0");

        // Sanity test - Empty tree
        filterManager.applyFilters(root, issuesFilteredRoot, new ScanTreeNode("0"));
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(0, root.getIssueCount());
        assertEquals(0, rootIssues.size());

        // Insert 'Minor' issue and 'MIT' license to node 1.
        one.setIssues(Sets.newHashSet(createIssue(Severity.Minor)));
        one.setLicenses(Sets.newHashSet(createLicense("MIT")));

        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);
        // Assert that the issues filtered tree have 1 issue and one node except the root
        rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(1, issuesFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, issuesFilteredRoot.getChildren().get(0).getIssueCount());
        assertEquals(1, issuesFilteredRoot.getChildren().get(0).getIssues().size());
        assertEquals(0, issuesFilteredRoot.getChildren().get(0).getChildren().size());

        // Assert that the license filtered tree have 1 license and one node except the root
        rootIssues = licenseFilteredRoot.processTreeIssues();
        assertEquals(1, licenseFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, licenseFilteredRoot.getChildren().get(0).getLicenses().size());
    }

    @Test(dependsOnMethods = {"testNoFilter"})
    public void testOneIssueFilter() {
        // Filter 'Minor' issues
        severitiesFilters.replace(Severity.Minor, false);
        ScanTreeNode issuesFilteredRoot = new ScanTreeNode("0");
        ScanTreeNode licenseFilteredRoot = new ScanTreeNode("0");
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the 'Minor' issue from 'testNoFilter' had been filtered
        Set<Issue> rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(0, issuesFilteredRoot.getIssueCount());
        assertEquals(0, rootIssues.size());

        // Assert that the license filtered tree have 1 license and one node except the root
        rootIssues = licenseFilteredRoot.processTreeIssues();
        assertEquals(1, licenseFilteredRoot.getIssueCount());
        assertEquals(1, rootIssues.size());
        assertEquals(1, licenseFilteredRoot.getChildren().get(0).getLicenses().size());
    }

    @Test(dependsOnMethods = {"testOneIssueFilter"})
    public void testManyIssueFilters() {
        // Filter 'Minor' and 'Major' issues
        severitiesFilters.replace(Severity.Major, false);
        ScanTreeNode issuesFilteredRoot = new ScanTreeNode("0");
        ScanTreeNode licenseFilteredRoot = new ScanTreeNode("0");

        // Insert some issues
        two.setIssues(Sets.newHashSet(createIssue(Severity.Major), createIssue(Severity.Critical)));
        four.setIssues(Sets.newHashSet(createIssue(Severity.Unknown)));
        five.setIssues(Sets.newHashSet(createIssue(Severity.Minor)));
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the issues filtered tree have 2 issues (1 critical and 1 unknown)
        Set<Issue> rootIssues = issuesFilteredRoot.processTreeIssues();
        assertEquals(2, issuesFilteredRoot.getIssueCount());
        assertEquals(2, rootIssues.size());
        rootIssues.forEach(issue -> {
            switch (issue.getComponent()) {
                case "2":
                    assertEquals(Severity.Critical, issue.getSeverity());
                    break;
                case "4":
                    assertEquals(Severity.Unknown, issue.getSeverity());
                    break;
                default:
                    fail("issues filtered tree should have only 1 critical issue and 1 unknown issue");
                    break;
            }
        });
    }

    @Test(dependsOnMethods = {"testNoFilter"})
    public void testOneLicenseFilter() {
        // Filter out all licenses
        licensesFilters.clear();
        ScanTreeNode issuesFilteredRoot = new ScanTreeNode("0");
        ScanTreeNode licenseFilteredRoot = new ScanTreeNode("0");
        filterManager.applyFilters(root, issuesFilteredRoot, licenseFilteredRoot);

        // Assert that the license in "1" have been filtered
        assertEquals(1, one.getLicenses().size());
        assertEquals(0, licenseFilteredRoot.getLicenses().size());
        assertEquals(0, licenseFilteredRoot.getChildren().size());
    }

    @Test(dependsOnMethods = {"testOneLicenseFilter"})
    public void testManyLicenseFilter() {
        // Accept 'MIT' and 'GPL' licenses
        licensesFilters.put(createLicense("MIT"), true);
        licensesFilters.put(createLicense("GPL"), true);

        // Insert some licenses
        two.setLicenses(Sets.newHashSet(createLicense("MIT"), createLicense("GNU")));
        three.setLicenses(Sets.newHashSet(createLicense("GNU")));
        five.setLicenses(Sets.newHashSet(createLicense("MIT")));
        ScanTreeNode licensesFilteredRoot = new ScanTreeNode("0");
        filterManager.applyFilters(root, new ScanTreeNode("0"), licensesFilteredRoot);

        // Assert that the license filtered tree root has 2 children ("2" and "3")
        assertEquals(2, licensesFilteredRoot.getChildren().size());
        licensesFilteredRoot.processTreeIssues();

        // Check each one of the license filtered tree nodes
        assertEquals(0, licensesFilteredRoot.getLicenses().size());
        assertEquals(2, licensesFilteredRoot.getChildren().size());
        licensesFilteredRoot.getChildren().forEach(child -> {
            if (child.getUserObject().equals("1")) {
                assertEquals(1, child.getLicenses().size());
            }
            if (child.getUserObject().equals("2")) {
                assertEquals(2, child.getLicenses().size());
                assertEquals(1, child.getChildren().size()); // "3" filtered out
                child.getChildren().forEach(twoChild -> {
                    // "4" have no licenses and should appear because of "5"
                    assertEquals(0, twoChild.getLicenses().size());
                    assertEquals(1, twoChild.getChildren().size());
                    twoChild.getChildren().forEach(fourChild -> {
                        // 5
                        assertEquals(1, fourChild.getLicenses().size());
                    });
                });
            }
        });
    }
}
