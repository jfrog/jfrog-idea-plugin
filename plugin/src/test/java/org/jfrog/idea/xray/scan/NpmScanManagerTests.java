package org.jfrog.idea.xray.scan;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.services.summary.Components;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.xray.ScanTreeNode;
import org.jfrog.idea.xray.utils.npm.NpmDriver;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.testng.collections.Sets;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.jfrog.idea.xray.scan.NpmScanManager.NPM_PREFIX;
import static org.jfrog.idea.xray.scan.NpmScanManager.findApplicationDirs;
import static org.jfrog.idea.xray.scan.ScanManager.getProjectBasePath;
import static org.testng.Assert.*;

/**
 * Created by Yahav Itzhak on 25 Dec 2017.
 */
public class NpmScanManagerTests {

    private Project project;
    private static final String FIRST_PACKAGE = "package-name1";
    private static final String SECOND_PACKAGE = "package-name2";
    private static final String DEBUG_COMPONENT_ID = "debug:3.1.0";
    private static final String SEND_COMPONENT_ID = "send:0.1.0";
    private static final List<String> DEBUG_COMPONENTS_IDS = Lists.newArrayList("ms:2.0.0");
    private static final List<String> SEND_COMPONENTS_IDS = Lists.newArrayList("debug:3.1.0", "fresh:0.1.0", "mime:1.2.6", "range-parser:0.0.4");

    @BeforeTest
    public void init() {
        project = new NpmProjectImpl();
        assertNotNull(project.getBasePath());
        NpmDriver npmDriver = new NpmDriver();
        try {
            npmDriver.install(project.getBasePath());
            npmDriver.install(Paths.get(project.getBasePath(), "a").toString());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @AfterTest
    public void terminate() {
        project.dispose();
    }

    @Test
    public void testIsApplicable() {
        Set<String> applicationDirs = Sets.newHashSet();
        Set<Path> applicationPaths = Sets.newHashSet();
        String basePath = getProjectBasePath(project);
        applicationPaths.add(Paths.get(basePath));
        try {
            applicationDirs = findApplicationDirs(applicationPaths);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertTrue(applicationDirs.contains(basePath));
        assertTrue(applicationDirs.contains(Paths.get(basePath, "a").toString()));
        assertTrue(NpmScanManager.isApplicable(applicationDirs));
    }

    /**
     * Test the full flow of NpmScanManager refresh dependencies.
     * The test project contains 2 components:
     * 'debug:3.1.0' - Contains 'ms:2.0.0'
     * 'send:0.1.0' - Contains 'debug:3.1.0', 'fresh:0.1.0', 'mime:1.2.6' and 'range-parser:0.0.4'
     * The test verify that the ScanTreeNode built as required with these components.
     */
    @Test
    public void testRefreshDependencies() {
        NpmScanManager scanManager = createNpmScanManager();
        assertNotNull(scanManager);
        scanManager.refreshDependencies(getRefreshDependenciesCbk(), null);
        assertEquals(scanManager.rootNode.getChildCount(), 2);
        scanManager.rootNode.getChildren().forEach(child -> {
            assertEquals(child.getChildCount(), 1);
            String packageName = child.getUserObject().toString();
            assertNotNull(packageName);
            ScanTreeNode dependency = child.getChildren().get(0);
            switch (packageName) {
                case FIRST_PACKAGE:
                    assertEquals(DEBUG_COMPONENTS_IDS.size(), dependency.getChildCount());
                    dependency.getChildren().forEach(debugChild -> {
                        String debugChildComponent = debugChild.getUserObject().toString();
                        assertTrue(DEBUG_COMPONENTS_IDS.contains(debugChildComponent));
                    });
                    break;

                case SECOND_PACKAGE:
                    assertEquals(SEND_COMPONENTS_IDS.size(), dependency.getChildCount());
                    dependency.getChildren().forEach(sendChild -> {
                        String sendChildComponent = sendChild.getUserObject().toString();
                        assertTrue(SEND_COMPONENTS_IDS.contains(sendChildComponent));
                    });
                    break;
                default:
                    fail("Wrong child " + packageName);
                    break;
            }
        });
    }

    /**
     * Test the components status before the Xray scan
     */
    @Test
    public void testCollectComponentsToScan() {
        NpmScanManager scanManager = createNpmScanManager();
        assertNotNull(scanManager);
        scanManager.refreshDependencies(getRefreshDependenciesCbk(), null);
        Components components = scanManager.collectComponentsToScan(null);
        assertEquals(components.getComponentDetails().size(), 6);
        components.getComponentDetails().forEach(componentDetail -> {
            String componentId = componentDetail.getComponentId();
            assertTrue(componentId.startsWith(NPM_PREFIX));
            componentId = componentId.substring(NPM_PREFIX.length());
            assertTrue(componentId.equals(DEBUG_COMPONENT_ID) ||
                    componentId.equals(SEND_COMPONENT_ID) ||
                    DEBUG_COMPONENTS_IDS.contains(componentId) ||
                    SEND_COMPONENTS_IDS.contains(componentId)
            );
        });
    }

    private ExternalProjectRefreshCallback getRefreshDependenciesCbk() {
        return new ExternalProjectRefreshCallback() {
            @Override
            public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
            }

            @Override
            public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
                fail(errorMessage + ": " + errorDetails);
            }
        };
    }

    private NpmScanManager createNpmScanManager() {
        Set<Path> applicationDirs = Sets.newHashSet();
        String appDir = getProjectBasePath(project);
        applicationDirs.add(Paths.get(appDir));
        try {
            return NpmScanManager.CreateNpmScanManager(project, findApplicationDirs(applicationDirs));
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return null;
    }
}