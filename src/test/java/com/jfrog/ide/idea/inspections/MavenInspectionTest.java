package com.jfrog.ide.idea.inspections;

import com.intellij.psi.xml.XmlTag;

/**
 * @author yahavi
 */
public class MavenInspectionTest extends InspectionsTest {

    private static final String PACKAGE_DESCRIPTOR = "pom.xml";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(789, "a", "b")
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {397, 549, 766};

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
