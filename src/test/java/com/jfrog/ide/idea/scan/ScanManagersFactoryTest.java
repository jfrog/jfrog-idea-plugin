package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.utils.Utils;
import junit.framework.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.intellij.testFramework.UsefulTestCase.assertContainsElements;
import static com.intellij.testFramework.UsefulTestCase.assertOneOf;

/**
 * @author yahavi
 **/
public class ScanManagersFactoryTest extends TestCase {
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
        scanManagersFactory.refreshScanManagers(Utils.ScanLogicType.GraphScan);
    }

    public void testRefreshScanManagers() {
        // Make sure there are 3 scan managers
        Collection<ScanManager> scanManagers = scanManagersFactory.scanManagers.values();
        assertEquals(3, scanManagers.size());

        // Keep the memory addresses of the previous scan cache and scan logic to make sure that:
        // 1. ScanCache is shared between all scan managers
        // 2. Each scan manager should contain a new instance of ScanLogic
        List<ScanLogic> logics = new ArrayList<>();
        List<ScanCache> caches = new ArrayList<>();
        for (ScanManager scanManager : scanManagers) {
            // Assert project name
            String projectName = Paths.get(scanManager.getProjectName()).getFileName().toString();
            assertOneOf(projectName, "project1", "project2", "subproject");

            // Save the ScanLogic and ScanCache to be compared later on
            GraphScanLogic scanLogic = (GraphScanLogic) scanManager.getScanLogic();
            logics.add(scanLogic);
            caches.add(scanLogic.getScanCache());
        }
        // Assert different ScanLogic object between all scan managers
        assertEquals(3, logics.stream().map(System::identityHashCode).distinct().count());
        // Assert shared ScanCache object between all scan managers
        assertEquals(1, caches.stream().map(System::identityHashCode).distinct().count());
    }

    public void testCreateScanPaths() {
        // Create scan paths
        Set<Path> scanPaths = scanManagersFactory.createScanPaths(scanManagersFactory.scanManagers);

        // Make sure project base path, all modules and submodule in path
        assertContainsElements(scanPaths, Utils.getProjectBasePath(myFixture.getProject()), PROJECT1, PROJECT2, PROJECT2.resolve("subproject"));
    }
}
