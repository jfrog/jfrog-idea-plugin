package com.jfrog.ide.idea.inspections;

import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

/**
 * @author yahavi
 */
public class GradleGroovyInspectionTest extends InspectionsTestBase {

    // We are setting 'build.groovy' instead pf 'build.gradle' since the testing FW doesn't identify 'build.gradle'
    // files as groovy-script.
    private static final String PACKAGE_DESCRIPTOR = "build.groovy";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(96, "a1", "b1"),
            new InspectionTestDependency(139, "a2", "b2"),
            new InspectionTestDependency(180, "a3", "b3"),
            new InspectionTestDependency(215, "d", "e"),
            new InspectionTestDependency(256, "a4", "b4"),
            new InspectionTestDependency(321, "project")
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {20, 287, 385};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GradleGroovyInspection(), PACKAGE_DESCRIPTOR, GrLiteral.class);
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
