package com.jfrog.ide.idea.exclusion;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.jfrog.ide.idea.TestUtils;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.junit.Assert;

import static com.jfrog.ide.common.utils.Utils.createComponentId;

/**
 * Created by Bar Belity on 10/06/2020.
 */
public class MavenExclusionTest extends LightJavaCodeInsightFixtureTestCase {

    PsiFile fileDescriptor;
    DependencyTree root, one, two, three, four, five;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        initTestingTree();
        fileDescriptor = myFixture.configureByFile("pom.xml");
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/exclusion";
    }

    private void initTestingTree() {
        root = new DependencyTree("node-root");
        one = createDependencyTreeNode("1", "maven");
        two = createDependencyTreeNode("2", "gradle");
        three = createDependencyTreeNode("3", "maven");
        four = createDependencyTreeNode("4", "maven");
        five = createDependencyTreeNode("5", "maven");
        root.add(one); // root -> 1
        root.add(two); // root -> 2
        one.add(three); // 1 -> 3
        two.add(four); // 2 -> 4
        four.add(five); // 4 -> 5
    }

    public void testIsMavenPackageType() {
        Assert.assertTrue("isMavenPackageType should be true on " + one,
                MavenExclusion.isMavenPackageType(one));

        Assert.assertTrue("isMavenPackageType should be true on " + five,
                MavenExclusion.isMavenPackageType(five));

        Assert.assertFalse("isMavenPackageType should be false on " + two,
                MavenExclusion.isMavenPackageType(two));

        Assert.assertFalse("isMavenPackageType should be false on " + root.toString(),
                MavenExclusion.isMavenPackageType(root));

        Assert.assertFalse("isMavenPackageType should be false on " + root.getParent(),
                MavenExclusion.isMavenPackageType((DependencyTree) root.getParent()));

        Assert.assertFalse("isMavenPackageType should be false on " + one.getParent(),
                MavenExclusion.isMavenPackageType((DependencyTree) one.getParent()));
    }

    public void testIsExcludable() {
        Assert.assertTrue("isExcludable should be true on " + three + " and " + one,
                MavenExclusion.isExcludable(three, one));

        Assert.assertFalse("isExcludable should be false on " + five + " and " + two,
                MavenExclusion.isExcludable(five, two));

        Assert.assertFalse("isExcludable should be false on " + one + " and " + one,
                MavenExclusion.isExcludable(two, two));
    }

    public void testExclusionExists() {
        ExclusionTestCase existingExclusion = new ExclusionTestCase(696, "group-id-3", "artifact-id-3");
        ExclusionTestCase nonExistingExclusion = new ExclusionTestCase(696, "group-id-4", "artifact-id-4");

        // Test exclusions of dependency 'group-id-2', 'artifact-id-2'.
        MavenExclusion exclusion = new MavenExclusion(null, null);
        PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, XmlTag.class, existingExclusion.offset);
        Assert.assertTrue("Found element should be of type XmlTag", element instanceof XmlTag);
        XmlTag exclusionsTag = ((XmlTag) element).findFirstSubTag(MavenExclusion.MAVEN_EXCLUSIONS_TAG);
        assertNotNull(exclusionsTag);
        XmlTag[] allExclusions = exclusionsTag.findSubTags(MavenExclusion.MAVEN_EXCLUSION_TAG);

        // Look for exclusion of existingExclusion.
        Assert.assertTrue(
                String.format("exclusionExists should be true for group-id: %s, artifact-id: %s for dependency:\n%s",
                        existingExclusion.groupId, existingExclusion.artifactId, element.getText()),
                exclusion.exclusionExists(allExclusions, existingExclusion.groupId, existingExclusion.artifactId));

        // Look for exclusion nonExistingExclusion.
        Assert.assertFalse(
                String.format("exclusionExists should be false for group-id: %s, artifact-id: %s for dependency:\n%s",
                        nonExistingExclusion.groupId, nonExistingExclusion.artifactId, element.getText()),
                exclusion.exclusionExists(allExclusions, nonExistingExclusion.groupId, nonExistingExclusion.artifactId));
    }

    public void testCreateAndAddExclusionTags() {
        ExclusionTestCase exclusionTestCase = new ExclusionTestCase(696, "group-id-4", "artifact-id-4");

        MavenExclusion exclusion = new MavenExclusion(null, null);
        PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, XmlTag.class, exclusionTestCase.offset);
        Assert.assertTrue("Found element should be of type XmlTag", element instanceof XmlTag);
        XmlTag exclusionsTag = ((XmlTag) element).findFirstSubTag(MavenExclusion.MAVEN_EXCLUSIONS_TAG);
        assertNotNull(exclusionsTag);
        XmlTag[] allExclusions = exclusionsTag.findSubTags(MavenExclusion.MAVEN_EXCLUSION_TAG);

        // Look for exclusion of testCase - should not be found.
        Assert.assertFalse(
                String.format("exclusionExists should be false for group-id: %s, artifact-id: %s for dependency:\n%s",
                        exclusionTestCase.groupId, exclusionTestCase.artifactId, element.getText()),
                exclusion.exclusionExists(allExclusions, exclusionTestCase.groupId, exclusionTestCase.artifactId));

        // Add exclusion of testCase.
        XmlFile xmlFileDescriptor = (XmlFile) fileDescriptor;
        WriteCommandAction.writeCommandAction(xmlFileDescriptor.getProject(), xmlFileDescriptor).run(() ->
                exclusion.createAndAddExclusionTags(exclusionsTag, exclusionTestCase.groupId, exclusionTestCase.artifactId));

        // Look for exclusion of testCase - should be found.
        allExclusions = exclusionsTag.findSubTags(MavenExclusion.MAVEN_EXCLUSION_TAG);
        Assert.assertTrue(
                String.format("exclusionExists should be true for group-id: %s, artifact-id: %s for dependency:\n%s",
                        exclusionTestCase.groupId, exclusionTestCase.artifactId, element.getText()),
                exclusion.exclusionExists(allExclusions, exclusionTestCase.groupId, exclusionTestCase.artifactId));
    }

    DependencyTree createDependencyTreeNode(String nodeValue, String pkgType) {
        DependencyTree node = new DependencyTree("node-" + nodeValue);
        node.setGeneralInfo(new GeneralInfo().pkgType(pkgType)
                .componentId(createComponentId("group-id-" + nodeValue, "artifact-id-" + nodeValue, "version-" + nodeValue)));
        return node;
    }

    static class ExclusionTestCase {
        // Parent dependency offset in pom.xml.
        private final int offset;

        // Exclusion's group-id.
        private final String groupId;

        // Exclusion's artifact-id.
        private final String artifactId;

        ExclusionTestCase(int offset, String groupId, String artifactId) {
            this.offset = offset;
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }
}
