package com.jfrog.ide.idea.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.QuickFix;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.LicenseViolationNode;
import com.jfrog.ide.common.nodes.VulnerabilityNode;
import com.jfrog.ide.common.nodes.subentities.Severity;
import com.jfrog.ide.idea.inspections.upgradeversion.UpgradeVersion;

import java.util.List;

/**
 * A license-policy watch puts license violations on the same dependency as vulnerabilities.
 * The in-editor inspection must keep reporting the dependency and must not offer a vulnerability upgrade for a license.
 */
public class LicensePolicyInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    public void testLicenseViolationDoesNotOfferUpgrade() {
        DependencyNode dependency = directDependency();
        dependency.addIssue(new LicenseViolationNode("Apache-2.0", "Apache-2.0", null, Severity.High, "", null));

        ProblemDescriptor problem = inspect(dependency);

        assertFalse(hasUpgrade(problem));
        assertEquals("JFrog: dependency <g:artifact:1.0> violates license policy (Apache-2.0)", problem.getDescriptionTemplate());
        assertEquals("Show in JFrog plugin - dependency <g:artifact:1.0>", showInTreeName(problem));
    }

    public void testVulnerabilityStillOffersUpgrade() {
        DependencyNode dependency = directDependency();
        dependency.addIssue(new VulnerabilityNode("XRAY-1", Severity.High, "summary", List.of("1.2.3"),
                null, null, "", null, null, null, null));

        ProblemDescriptor problem = inspect(dependency);

        assertTrue(hasUpgrade(problem));
        assertEquals("JFrog: dependency <g:artifact:1.0> has security vulnerabilities", problem.getDescriptionTemplate());
        assertEquals("Show in JFrog plugin - dependency <g:artifact:1.0>", showInTreeName(problem));
    }

    public void testVulnerabilityAndLicenseNamesBoth() {
        DependencyNode dependency = directDependency();
        dependency.addIssue(new VulnerabilityNode("XRAY-1", Severity.High, "summary", List.of("1.2.3"),
                null, null, "", null, null, null, null));
        dependency.addIssue(new LicenseViolationNode("Apache License 2.0", "Apache-2.0", null, Severity.High, "", null));

        ProblemDescriptor problem = inspect(dependency);

        assertTrue(hasUpgrade(problem));
        assertEquals("JFrog: dependency <g:artifact:1.0> has security vulnerabilities and violates license policy (Apache-2.0)",
                problem.getDescriptionTemplate());
    }

    public void testMultipleLicenseViolationsAreNamedOnce() {
        DependencyNode dependency = directDependency();
        dependency.addIssue(new LicenseViolationNode("MIT License", "MIT", null, Severity.High, "", null));
        dependency.addIssue(new LicenseViolationNode("GNU General Public License v2.0", "GPL-2.0", null, Severity.Medium, "", null));
        dependency.addIssue(new LicenseViolationNode("MIT License", "MIT", null, Severity.High, "", null));

        ProblemDescriptor problem = inspect(dependency);

        assertFalse(hasUpgrade(problem));
        assertEquals("JFrog: dependency <g:artifact:1.0> violates license policy (MIT, GPL-2.0)", problem.getDescriptionTemplate());
    }

    public void testUnnamedLicenseViolationOmitsParentheses() {
        DependencyNode dependency = directDependency();
        dependency.addIssue(new LicenseViolationNode("", "", null, Severity.High, "", null));

        ProblemDescriptor problem = inspect(dependency);

        assertEquals("JFrog: dependency <g:artifact:1.0> violates license policy", problem.getDescriptionTemplate());
    }

    public void testTransitiveLicenseViolationKeepsTransitiveWording() {
        DependencyNode dependency = directDependency();
        dependency.setIndirect(true);
        dependency.addIssue(new LicenseViolationNode("MIT License", "MIT", null, Severity.High, "", null));

        ProblemDescriptor problem = inspect(dependency);

        assertFalse(hasUpgrade(problem));
        assertEquals("JFrog: transitive dependency <g:artifact:1.0> violates license policy (MIT)", problem.getDescriptionTemplate());
    }

    private DependencyNode directDependency() {
        return new DependencyNode().componentId("gav://g:artifact:1.0");
    }

    private ProblemDescriptor inspect(DependencyNode dependency) {
        PsiFile file = myFixture.configureByText("pom.xml", "<project></project>");
        ProblemsHolder holder = new ProblemsHolder(InspectionManager.getInstance(getProject()), file, true);
        new MavenInspection().registerProblem(holder, dependency, file, "artifact");
        assertEquals(1, holder.getResults().size());
        return holder.getResults().get(0);
    }

    private static boolean hasUpgrade(ProblemDescriptor problem) {
        for (QuickFix<?> fix : problem.getFixes()) {
            if (fix instanceof UpgradeVersion) {
                return true;
            }
        }
        return false;
    }

    private static String showInTreeName(ProblemDescriptor problem) {
        for (QuickFix<?> fix : problem.getFixes()) {
            if (fix instanceof ShowInDependencyTree show) {
                return show.getFamilyName();
            }
        }
        return null;
    }
}
