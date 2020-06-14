package com.jfrog.ide.idea.inspections;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.jfrog.ide.idea.TestUtils;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.junit.Assert;

/**
 * @author yahavi
 */
public abstract class InspectionsTest extends LightJavaCodeInsightFixtureTestCase {

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

    public void isDependencyTest(Object[][] dependencies) {
        for (Object[] dependency : dependencies) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, (int) dependency[0]);
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

    public void createGeneralInfoTest(Object[][] dependencies) {
        for (Object[] dependency : dependencies) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, (int) dependency[0]);
            GeneralInfo generalInfo = inspection.createGeneralInfo(element);
            Assert.assertNotNull(generalInfo);
            Assert.assertEquals(generalInfoToString(generalInfo), generalInfoToString(createGeneralInfo(dependency)));
            Assert.assertTrue(inspection.compareGeneralInfos(generalInfo, createGeneralInfo(dependency)));
        }
    }

    GeneralInfo createGeneralInfo(Object[] dependency) {
        return new GeneralInfo()
                .groupId((String) dependency[1])
                .artifactId((String) dependency[2]);
    }

    private String generalInfoToString(GeneralInfo generalInfo) {
        return String.join(":", generalInfo.getGroupId(), generalInfo.getArtifactId());
    }
}
