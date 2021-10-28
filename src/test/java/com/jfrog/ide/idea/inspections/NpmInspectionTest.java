package com.jfrog.ide.idea.inspections;

import com.intellij.json.psi.JsonProperty;

/**
 * @author yahavi
 */
public class NpmInspectionTest extends InspectionsTest {

    private static final String PACKAGE_DESCRIPTOR = "package.json";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(67, "a"),
            new InspectionTestDependency(82, "c"),
            new InspectionTestDependency(128, "a")
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {16, 36};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new NpmInspection(), PACKAGE_DESCRIPTOR, JsonProperty.class);
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
