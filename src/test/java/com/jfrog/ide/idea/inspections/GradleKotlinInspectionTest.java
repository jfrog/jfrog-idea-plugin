package com.jfrog.ide.idea.inspections;

import org.jetbrains.kotlin.psi.KtValueArgumentList;

/**
 * @author yahavi
 */
public class GradleKotlinInspectionTest extends InspectionsTestBase {

    // We are setting 'build.groovy' instead pf 'build.gradle' since the testing FW doesn't identify 'build.gradle'
    // files as groovy-script.
    private static final String PACKAGE_DESCRIPTOR = "build.gradle.kts";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(119, "a", "b"),
            new InspectionTestDependency(144, "d", "e"),
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {273, 338};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GradleKotlinInspection(), PACKAGE_DESCRIPTOR, KtValueArgumentList.class);
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
