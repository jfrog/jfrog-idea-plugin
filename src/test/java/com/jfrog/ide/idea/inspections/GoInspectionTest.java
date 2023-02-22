package com.jfrog.ide.idea.inspections;

import com.goide.vgo.mod.psi.VgoModuleSpec;

/**
 * Created by Bar Belity on 23/02/2020.
 */
public class GoInspectionTest extends InspectionsTestBase {

    private static final String PACKAGE_DESCRIPTOR = "go.mod";
    private final InspectionTestDependency[] DEPENDENCIES = {
            new InspectionTestDependency(54, "github.com/jfrog/gocmd:0.1.12"),
            new InspectionTestDependency(89, "github.com/jfrog/gofrog:1.0.5"),
            new InspectionTestDependency(124, "github.com/jfrog/gogopowerrangers:1.2.3")
    };

    private final int[] NON_DEPENDENCIES_POSITIONS = {176, 202};

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GoInspection(), PACKAGE_DESCRIPTOR, VgoModuleSpec.class);
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
