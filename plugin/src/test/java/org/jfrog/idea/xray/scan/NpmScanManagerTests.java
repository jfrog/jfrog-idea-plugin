package org.jfrog.idea.xray.scan;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.jfrog.xray.client.services.summary.Components;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.jfrog.idea.xray.scan.NpmScanManager.NPM_PREFIX;
import static org.jfrog.idea.xray.scan.ScanManager.getProjectBasePath;
import static org.testng.Assert.*;

/**
 * Created by Yahav Itzhak on 25 Dec 2017.
 */
public class NpmScanManagerTests {

    private Project project;
    private NpmScanManager scanManager;
    private static final String DEBUG_COMPONENT_ID = "debug:3.1.0";
    private static final String SEND_COMPONENT_ID = "send:0.1.0";
    private static final List<String> DEBUG_COMPONENTS_IDS = Lists.newArrayList("ms:2.0.0");
    private static final List<String> SEND_COMPONENTS_IDS = Lists.newArrayList("debug:3.1.0", "fresh:0.1.0", "mime:1.2.6", "range-parser:0.0.4");

    @BeforeTest
    public void initProject() {
        project = new NpmProjectImpl();
        try {
            scanManager = NpmScanManager.CreateNpmScanManager(project);
        } catch (IOException e) {
            fail("Fail to create NpmScanManager", e);
        }
    }

    @Test
    public void testIsApplicable() {
        assertTrue(NpmScanManager.isApplicable(project));
    }

    /**
     * Test the full flow of NpmScanManager refresh dependencies.
     * The test project contains 2 components:
     * 'debug:3.1.0' - Contains 'ms:2.0.0'
     * 'send:0.1.0' - Contains 'debug:3.1.0', 'fresh:0.1.0', 'mime:1.2.6' and 'range-parser:0.0.4'
     * The test verify that the ScanTreeNode built as required with these components.
     */
    @Test(dependsOnMethods = {"testIsApplicable"})
    public void testRefreshDependencies() {
        scanManager.refreshDependencies(getRefreshDependenciesCbk(), null);
        assertEquals(2, scanManager.rootNode.getChildCount());
        scanManager.rootNode.getChildren().forEach(child -> {
            String childComponent = child.getUserObject().toString();
            assertNotNull(childComponent);
            switch (childComponent) {
                case DEBUG_COMPONENT_ID:
                    assertEquals(DEBUG_COMPONENTS_IDS.size(), child.getChildCount());
                    child.getChildren().forEach(debugChild -> {
                        String debugChildComponent = debugChild.getUserObject().toString();
                        assertTrue(DEBUG_COMPONENTS_IDS.contains(debugChildComponent));
                    });
                    break;
                case SEND_COMPONENT_ID:
                    assertEquals(SEND_COMPONENTS_IDS.size(), child.getChildCount());
                    child.getChildren().forEach(sendChild -> {
                        String sendChildComponent = sendChild.getUserObject().toString();
                        assertTrue(SEND_COMPONENTS_IDS.contains(sendChildComponent));
                    });
                    break;
                default:
                    fail("Wrong child " + childComponent);
                    break;
            }
        });
    }

    /**
     * Test the components status before the Xray scan
     */
    @Test(dependsOnMethods = {"testRefreshDependencies"})
    public void testCollectComponentsToScan() {
        Components components = scanManager.collectComponentsToScan(null);
        assertEquals(6, components.getComponentDetails().size());
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

    @AfterClass
    public void cleanProject() {
        try {
            Path installationDirectory = Paths.get(getProjectBasePath(project), ".idea");
            if (Files.exists(installationDirectory)) {
                FileUtils.forceDelete(installationDirectory.toFile());
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}