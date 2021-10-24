package com.jfrog.ide.idea.inspections;

import org.jetbrains.kotlin.psi.KtValueArgumentList;

/**
 * @author yahavi
 */
public class GradleKotlinInspectionTest extends InspectionsTest {

    // We are setting 'build.groovy' instead pf 'build.gradle' since the testing FW doesn't identify 'build.gradle'
    // files as groovy-script.
    private static final String PACKAGE_DESCRIPTOR = "build.gradle.kts";
    private final Object[][] DEPENDENCIES = {
            // offset, groupId, artifactId
            {96, "", "project"},
            {119, "a", "b"},
            {144, "d", "e"},
            {147, "d", "e"},
            {155, "d", "e"},
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
