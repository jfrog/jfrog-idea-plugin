package org.jfrog.idea.xray;

import com.google.common.collect.Sets;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.persistency.types.Severity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

public class ScanTreeNodeTests extends ScanTreeNodeBase {

    @BeforeClass
    public void init() {
        super.init();
    }

    @Test
    public void testInit() {
        // Sanity test - Check tree with no issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertTrue(rootIssues.isEmpty());
        assertEquals(Severity.Normal, root.getTopIssue().severity);
    }

    @Test(dependsOnMethods = {"testInit"})
    public void testOneNode() {
        // Populate "1" with one empty issue and one empty license.
        Set<Issue> oneIssues = Sets.newHashSet(new Issue());
        Set<License> oneLicenses = Sets.newHashSet(new License());
        one.setIssues(oneIssues);
        one.setLicenses(oneLicenses);

        // Assert the tree has 1 issue
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(1, rootIssues.size());
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[0]).severity);

        // Check isHigherSeverityThan() functionality
        assertTrue(createIssue(Severity.Unknown).isHigherSeverityThan(root.getTopIssue()));
    }

    @Test(dependsOnMethods = {"testOneNode"})
    public void testTwoNodes() {
        // Populate node two with one empty issue and one empty license.
        Set<Issue> twoIssues = Sets.newHashSet(createIssue(Severity.Normal));
        Set<License> twoLicenses = Sets.newHashSet(new License());
        two.setIssues(twoIssues);
        two.setLicenses(twoLicenses);

        // Assert the tree has 2 issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(2, rootIssues.size());
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[0]).severity);
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[1]).severity);
        assertTrue(createIssue(Severity.Unknown).isHigherSeverityThan(root.getTopIssue()));
    }

    @Test(dependsOnMethods = {"testTwoNodes"})
    public void testFourNodes() {
        // Populate node three with one minor issue
        Issue threeIssue = createIssue(Severity.Minor);
        three.setIssues(Sets.newHashSet(threeIssue));
        three.setLicenses(Sets.newHashSet());
        three.processTreeIssues();

        // Assert the tree has 3 issues
        assertEquals(Severity.Minor, three.getTopIssue().severity);
        assertEquals("3", three.getTopIssue().component);

        // Populate node four with minor and major issues
        Issue fourFirstIssue = createIssue(Severity.Major);
        Issue fourSecondIssue = createIssue(Severity.Minor);
        four.setIssues(Sets.newHashSet(fourFirstIssue, fourSecondIssue));

        // Assert the tree has 5 issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(5, rootIssues.size());
        assertEquals(fourFirstIssue, root.getTopIssue());
    }

    @Test(dependsOnMethods = {"testFourNodes"})
    public void testFiveNodes() {
        // Populate node five with 6 issues
        five.setIssues(Sets.newHashSet(createIssue(Severity.Normal),
                createIssue(Severity.Minor),
                createIssue(Severity.Minor),
                createIssue(Severity.Unknown),
                createIssue(Severity.Critical),
                createIssue(Severity.Major)));

        // Assert that all issues are in the tree
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(11, rootIssues.size());
        assertEquals(Severity.Critical, root.getTopIssue().severity);
        assertEquals("5", root.getTopIssue().component);
        assertEquals("", one.getTopIssue().component);
        assertEquals("5", two.getTopIssue().component);
        assertEquals("3", three.getTopIssue().component);
        assertEquals("5", four.getTopIssue().component);
        assertEquals("5", five.getTopIssue().component);
    }
}