package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.common.scan.ScanLogic;
import com.jfrog.ide.idea.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yahavi
 **/
public class ScanManagersFactoryTest extends HeavyPlatformTestCase {
    private static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "projects"));
    private ScanManagersFactory scanManagersFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createTestProjectStructure(PROJECT_ROOT.toString());
        scanManagersFactory = ScanManagersFactory.getInstance(getProject());
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

    public void testCreateScanPaths() throws IOException {
        // Create scan paths
        Set<Path> scanPaths = scanManagersFactory.createScanPaths(scanManagersFactory.scanManagers);

        // Make sure all directories under the project base paths are included in the scan paths
        try (Stream<Path> files = Files.walk(Utils.getProjectBasePath(getProject()))) {
            Set<Path> expected = files.filter(file -> file.toFile().isDirectory()).collect(Collectors.toSet());
            assertEquals(expected, scanPaths);
        }
    }
}
