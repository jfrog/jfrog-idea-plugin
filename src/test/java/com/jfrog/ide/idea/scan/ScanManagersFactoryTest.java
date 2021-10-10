package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.jfrog.ide.common.persistency.ScanCache;
import com.jfrog.ide.common.scan.GraphScanLogic;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * @author yahavi
 **/
public class ScanManagersFactoryTest extends HeavyPlatformTestCase {
    private static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "projects"));

    Path projectDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        projectDir = getTempDir().createDir();
        FileUtils.copyDirectory(PROJECT_ROOT.toFile(), projectDir.toFile());
        createTestProjectStructure(Files.createTempDirectory("ScanManagersFactoryTest").toString());
    }

    public void testRefreshScanManagers() throws IOException {
        // Run refresh scan managers
        ScanManagersFactory scanManagersFactory = ScanManagersFactory.getInstance(getProject());
        scanManagersFactory.refreshScanManagers(Utils.ScanLogicType.GraphScan);

        // Make sure there are 3 scan managers
        Collection<ScanManager> scanManagers = scanManagersFactory.scanManagers.values();
        assertEquals(3, scanManagers.size());

        // Keep the memory addresses of the previous scan cache and scan logic to make sure that:
        // 1. ScanCache is shared between all scan managers
        // 2. Each scan manager should contain a new instance of ScanLogic
        GraphScanLogic previousScanLogic = null;
        ScanCache previousScanCache = null;
        for (ScanManager scanManager : scanManagers) {
            // Assert project name
            String projectName = Paths.get(scanManager.getProjectName()).getFileName().toString();
            assertOneOf(projectName, "project1", "project2", "subproject");

            // Assert shared ScanCache and different ScanLogic by comparing memory addresses
            GraphScanLogic scanLogic = (GraphScanLogic) scanManager.getScanLogic();
            ScanCache scanCache = scanLogic.getScanCache();
            assertNotSame(previousScanLogic, scanLogic);
            if (previousScanCache != null) {
                assertSame(scanCache, previousScanCache);
            }
            previousScanLogic = scanLogic;
            previousScanCache = scanCache;
        }
    }
}
