package com.jfrog.ide.idea.inspections;

import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.TestUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.junit.Assert;

import java.util.List;

/**
 * @author yahavi
 */
public class GradleGroovyInspectionTest extends InspectionsTestBase {

    // We are setting 'build.groovy' instead pf 'build.gradle' since the testing FW doesn't identify 'build.gradle'
    // files as groovy-script.
    private static final String PACKAGE_DESCRIPTOR = "build.groovy";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(96, "a", "b"),
            new InspectionTestDependency(139, "d", "e"),
            new InspectionTestDependency(180, "g", "h"),
            new InspectionTestDependency(200, "j", "k"),
            new InspectionTestDependency(225, "m", "n"),
            new InspectionTestDependency(320, "net.lingala.zip4j", "zip4j"),
            new InspectionTestDependency(390, "org.codehaus.groovy", "groovy-all"),
    };

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GradleGroovyInspection(), PACKAGE_DESCRIPTOR, GrArgumentList.class);
    }

    public void testCreateGeneralInfo() {
        for (InspectionTestDependency dependency : DEPENDENCIES) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, psiClass, dependency.offset);
            List<GroovyPsiElement> elementsToVisit = ((GradleGroovyInspection) inspection).parseComponentElements((GrArgumentList) element);
            for (GroovyPsiElement elementToVisit : elementsToVisit) {
                if (elementToVisit.getText().contains(dependency.groupId)) {
                    String componentName = inspection.createComponentName(elementToVisit);
                    Assert.assertNotNull(componentName);
                    assertEquals(String.join(":", dependency.groupId, dependency.artifactId), componentName);
                }
            }
        }
    }
}
