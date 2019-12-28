package com.jfrog.ide.idea.inspections;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jfrog.build.extractor.scan.GeneralInfo;
import org.junit.Assert;

/**
 * @author yahavi
 */
public abstract class InspectionsTest extends LightCodeInsightFixtureTestCase {

    PsiFile fileDescriptor;
    AbstractInspection inspection;
    private Class<? extends PsiElement> psiClass;

    public void setUp(AbstractInspection inspection, Class<? extends PsiElement> psiClass) throws Exception {
        super.setUp();
        this.inspection = inspection;
        this.psiClass = psiClass;
        fileDescriptor = myFixture.configureByFile("testData/" + inspection.getPackageDescriptorName());
    }

    PsiElement getNonLeafElement(int position) {
        PsiElement element = fileDescriptor.findElementAt(position);
        Assert.assertNotNull(element);
        while (!(psiClass.isAssignableFrom(element.getClass()))) {
            element = element.getParent();
            Assert.assertNotNull(element);
        }
        return element;
    }

    public void isDependencyTest(Object[][] dependencies) {
        for (Object[] dependency : dependencies) {
            PsiElement element = getNonLeafElement((int) dependency[0]);
            Assert.assertTrue("isDependency should be true on " + element.getText(),
                    inspection.isDependency(element));
        }
    }

    public void isNonDependencyTest(int[] nonDependenciesOffsets) {
        for (int position : nonDependenciesOffsets) {
            PsiElement element = getNonLeafElement(position);
            Assert.assertFalse(inspection.isDependency(element));
        }
    }

    public void createGeneralInfoTest(Object[][] dependencies) {
        for (Object[] dependency : dependencies) {
            PsiElement element = getNonLeafElement((int) dependency[0]);
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
