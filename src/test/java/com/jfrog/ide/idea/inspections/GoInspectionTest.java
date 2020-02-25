package com.jfrog.ide.idea.inspections;

import com.goide.vgo.mod.psi.VgoModuleSpec;

/**
 * Created by Bar Belity on 23/02/2020.
 */
public class GoInspectionTest extends InspectionsTest {

    private static final String PACKAGE_DESCRIPTOR = "go.mod";
    private final Object[][] DEPENDENCIES = {
        // offset, groupId, artifactId
        {54, "", "github.com/jfrog/gocmd"},
        {89, "", "github.com/jfrog/gofrog"},
        {124, "", "github.com/jfrog/gogopowerrangers"}
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
        createGeneralInfoTest(DEPENDENCIES);
    }
}
