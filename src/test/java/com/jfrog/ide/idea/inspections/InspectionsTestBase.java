package com.jfrog.ide.idea.inspections;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.jfrog.ide.idea.TestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

/**
 * @author yahavi
 */
public abstract class InspectionsTestBase extends LightJavaCodeInsightFixtureTestCase {

    PsiFile fileDescriptor;
    AbstractInspection inspection;
    private Class<? extends PsiElement> psiClass;

    public void setUp(AbstractInspection inspection, String packageDescriptorName, Class<? extends PsiElement> psiClass) throws Exception {
        super.setUp();
        this.fileDescriptor = myFixture.configureByFile(packageDescriptorName);
        this.inspection = inspection;
        this.psiClass = psiClass;
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/inspections";
    }

    public void isDependencyTest(InspectionTestDependency[] dependencies) {
        for (InspectionTestDependency dependency : dependencies) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, dependency.offset);
            Assert.assertTrue("isDependency should be true on " + element.getText(),
                    inspection.isDependency(element));
        }
    }

    public void isNonDependencyTest(int[] nonDependenciesOffsets) {
        for (int position : nonDependenciesOffsets) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, position);
            Assert.assertFalse(inspection.isDependency(element));
        }
    }

    public void createComponentNameTest(InspectionTestDependency[] dependencies) {
        for (InspectionTestDependency dependency : dependencies) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, dependency.offset);
            String componentName = inspection.createComponentName(element);
            Assert.assertNotNull(componentName);
            String expectedGroupId = dependency.groupId;
            String expectedArtifactId = dependency.artifactId;
            if (StringUtils.isBlank(expectedGroupId)) {
                assertEquals(expectedArtifactId, componentName);
            } else {
                assertEquals(String.join(":", expectedGroupId, expectedArtifactId), componentName);
            }
        }
    }

    static class InspectionTestDependency {
        private final String artifactId;
        private final int offset;
        private String groupId;

        public InspectionTestDependency(int offset, String groupId, String artifactId) {
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.offset = offset;
        }

        public InspectionTestDependency(int offset, String artifactId) {
            this.artifactId = artifactId;
            this.offset = offset;
        }
    }
}
