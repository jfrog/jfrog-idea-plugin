package com.jfrog.ide.idea.inspections;

import com.intellij.psi.xml.XmlTag;

/**
 * @author yahavi
 */
public class MavenInspectionTest extends InspectionsTestBase {

    private static final String PACKAGE_DESCRIPTOR = "pom.xml";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(550, "a", "b"),
            new InspectionTestDependency(788, "d", "e"),
            new InspectionTestDependency(990, "g", "h"),
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {397, 1197, 1258};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new MavenInspection(), PACKAGE_DESCRIPTOR, XmlTag.class);
    }

    public void testDependencies() {
        isDependencyTest(DEPENDENCIES);
    }

    public void testNonDependencies() {
        isNonDependencyTest(NON_DEPENDENCIES_POSITIONS);
    }

    public void testCreateGeneralInfo() {
        createComponentNameTest(DEPENDENCIES);
    }
}
