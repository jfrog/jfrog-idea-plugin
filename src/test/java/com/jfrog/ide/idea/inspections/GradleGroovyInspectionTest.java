package com.jfrog.ide.idea.inspections;

import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.TestUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
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
            new InspectionTestDependency(96, "a", "b:c"),
            new InspectionTestDependency(139, "d", "e:f"),
            new InspectionTestDependency(180, "g", "h:i"),
            new InspectionTestDependency(205, "j", "k:l"),
            new InspectionTestDependency(235, "m", "n:o"),
            new InspectionTestDependency(320, "net.lingala.zip4j", "zip4j:2.3.0"),
            new InspectionTestDependency(390, "org.codehaus.groovy", "groovy-all:3.0.5"),
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {20, 57, 435};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GradleGroovyInspection(), PACKAGE_DESCRIPTOR, GrNamedArgumentsOwner.class);
    }

    public void testDependencies() {
        isDependencyTest(DEPENDENCIES);
    }

    public void testNonDependencies() {
        isNonDependencyTest(NON_DEPENDENCIES_POSITIONS);
    }

    public void testCreateGeneralInfo() {
        for (InspectionTestDependency dependency : DEPENDENCIES) {
            PsiElement element = TestUtils.getNonLeafElement(fileDescriptor, GrArgumentList.class, dependency.offset);
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
