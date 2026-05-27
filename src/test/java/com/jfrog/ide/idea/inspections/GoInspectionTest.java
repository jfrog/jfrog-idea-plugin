package com.jfrog.ide.idea.inspections;

import com.goide.vgo.mod.psi.VgoModuleSpec;
import com.intellij.psi.PsiElement;
import com.jfrog.ide.idea.TestUtils;

import java.util.List;

/**
 * Created by Bar Belity on 23/02/2020.
 */
public class GoInspectionTest extends InspectionsTestBase {

    private static final String PACKAGE_DESCRIPTOR = "go.mod";

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public void setUp() throws Exception {
        super.setUp(new GoInspection(), PACKAGE_DESCRIPTOR, VgoModuleSpec.class);
    }

    public void testDependencies() {
        assertDependency("github.com/jfrog/gocmd");
        assertDependency("github.com/jfrog/gofrog");
        assertDependency("github.com/jfrog/gogopowerrangers");
    }

    public void testNonDependencies() {
        List<VgoModuleSpec> nonDependencies = TestUtils.findElementsOfType(fileDescriptor, VgoModuleSpec.class).stream()
                .filter(spec -> !inspection.isDependency(spec))
                .toList();
        assertFalse("Expected module specs in replace clause", nonDependencies.isEmpty());
        for (VgoModuleSpec spec : nonDependencies) {
            assertFalse("replace line should not be a dependency: " + spec.getText(), inspection.isDependency(spec));
        }
    }

    public void testCreateGeneralInfo() {
        assertComponentName("github.com/jfrog/gocmd", "github.com/jfrog/gocmd:0.1.12");
        assertComponentName("github.com/jfrog/gofrog", "github.com/jfrog/gofrog:1.0.5");
        assertComponentName("github.com/jfrog/gogopowerrangers", "github.com/jfrog/gogopowerrangers:1.2.3");
    }

    private void assertDependency(String modulePath) {
        PsiElement element = TestUtils.findElementByContainingText(fileDescriptor, VgoModuleSpec.class, modulePath);
        assertTrue("isDependency should be true on " + element.getText(), inspection.isDependency(element));
    }

    private void assertComponentName(String modulePath, String expected) {
        List<VgoModuleSpec> matches = TestUtils.findElementsOfType(fileDescriptor, VgoModuleSpec.class).stream()
                .filter(spec -> spec.getText().contains(modulePath) && inspection.isDependency(spec))
                .toList();
        assertEquals(1, matches.size());
        assertEquals(expected, inspection.createComponentName(matches.get(0)));
    }
}
