package com.jfrog.ide.idea.scan;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.jfrog.ide.idea.ProgressIndicatorMock;
import com.jfrog.ide.idea.utils.Utils;
import junit.framework.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static com.intellij.testFramework.UsefulTestCase.*;
import static com.intellij.testFramework.UsefulTestCase.assertEmpty;
import static com.jfrog.ide.idea.scan.ScanUtils.createScanPaths;
import static com.jfrog.ide.idea.scan.ScanUtils.isLocalProjectSupported;

/**
 * @author yahavi
 **/
public class ScanManagersTest extends TestCase {
    private static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "projects"));
    private static final Path PROJECT1 = PROJECT_ROOT.resolve("project1");
    private static final Path PROJECT2 = PROJECT_ROOT.resolve("project2");

    private ScanManagersFactory scanManagersFactory;
    private IdeaProjectTestFixture myFixture;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = JavaTestFixtureFactory.createFixtureBuilder(getName());
        fixtureBuilder.addModule(JavaModuleFixtureBuilder.class).addContentRoot(PROJECT1.toString());
        fixtureBuilder.addModule(JavaModuleFixtureBuilder.class).addContentRoot(PROJECT2.toString());

        myFixture = fixtureBuilder.getFixture();
        myFixture.setUp();
        scanManagersFactory = ScanManagersFactory.getInstance(myFixture.getProject());
        scanManagersFactory.refreshScanManagers(Utils.ScanLogicType.GraphScan, null);
    }

    @Override
    protected void tearDown() throws Exception {
        tearDownFixture(myFixture);
        super.tearDown();
    }

    public void testIsLocalProjectSupported() throws Exception {
        // Make sure isLocalProjectSupported return true on our project
        assertTrue(isLocalProjectSupported(myFixture.getProject()));

        // Make sure isLocalProjectSupported return false on a new project
        IdeaProjectTestFixture newFixture = JavaTestFixtureFactory.createFixtureBuilder(getName()).getFixture();
        newFixture.setUp();
        try {
            assertFalse(isLocalProjectSupported(newFixture.getProject()));
        } finally {
            tearDownFixture(newFixture);
        }
    }

    public void testScanManagersScanPaths() {
        // Create scan paths
        Set<Path> scanPaths = createScanPaths(scanManagersFactory.scanManagers, myFixture.getProject());

        // Make sure project base path, all modules and submodule in path
        assertContainsElements(scanPaths, Utils.getProjectBasePath(myFixture.getProject()), PROJECT1, PROJECT2, PROJECT2.resolve("subproject"));
    }

    private void tearDownFixture(IdeaProjectTestFixture fixture) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                fixture.tearDown();
            } catch (Exception ignore) {
            }
        });
    }

    public void testSourceCodeScanNoBinaries() {
        Collection<ScanManager> scanManagers = scanManagersFactory.scanManagers.values();
        assertNotEmpty(scanManagers);
        scanManagers.forEach(scanManager -> {
            try {
                scanManager.scanner.scanAndUpdate(new ProgressIndicatorMock(), new ArrayList<>());
                assertEmpty(scanManager.getSourceCodeScanResults());
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
    }
}
