package com.jfrog.ide.idea.scan;

import com.google.common.collect.Sets;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ConcurrencyUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;

import javax.swing.tree.TreeNode;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class YarnScannerTest extends HeavyPlatformTestCase {
    private static final Path YARN_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "yarn"));
    private ExecutorService executorService;
    private String tempProjectDir;

    public enum Project {
        EXAMPLE("exampleYarnPackage"),
        MONOREPO("exampleYarnMonorepo");

        private final String name;

        Project(String name) {
            this.name = name;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        executorService = ConcurrencyUtil.newSameThreadExecutorService();
    }


    @Override
    protected void tearDown() throws Exception {
        try {
            executorService.shutdown();
        } finally {
            // Ensure that tearDown gets executed even if an exception is thrown
            super.tearDown();
        }
    }

    private String createTempProjectDir(String projectName) throws IOException {
        // Using a virtual directory allows each test to have its own isolated workspace, preventing interference between tests.
        String tempProjectDir = getTempDir().createVirtualDir(projectName).toNioPath().toString();
        FileUtils.copyDirectory(YARN_ROOT.resolve(projectName).toFile(), FileUtils.getFile(tempProjectDir));
        return tempProjectDir;
    }

    public void testGetPackageNameToVersionsMap() throws IOException {
        tempProjectDir = createTempProjectDir(Project.EXAMPLE.name);
        YarnScanner yarnScanner = new YarnScanner(getProject(), tempProjectDir, executorService, new GraphScanLogic(new NullLog()));
        Set<String> packages = new HashSet<>(Arrays.asList("package1:1.0.123", "package2:2.0.9", "package1:1.0.123", "package2:2.1.7"));
        Map<String, Set<String>> packageNameToVersions = yarnScanner.getPackageNameToVersionsMap(packages);

        assertEquals(2, packageNameToVersions.size());
        assertTrue(packageNameToVersions.containsKey("package1"));
        assertTrue(packageNameToVersions.containsKey("package2"));
        assertFalse(packageNameToVersions.containsKey("package3"));

        assertEquals(Sets.newHashSet("1.0.123"), packageNameToVersions.get("package1"));
        assertEquals(Sets.newHashSet("2.0.9", "2.1.7"), packageNameToVersions.get("package2"));
    }

    private Map<String, DependencyNode> vulnerableDependenciesMapInit(String[] vulnerableDependencies) {
        Map<String, DependencyNode> vulnerableDependenciesMap = new HashMap<>();
        for (String vulnerableDependency : vulnerableDependencies) {
            DependencyNode dependencyNode = new DependencyNode();
            dependencyNode.componentId(vulnerableDependency);
            vulnerableDependenciesMap.put(vulnerableDependency, dependencyNode);
        }
        return vulnerableDependenciesMap;
    }

    private void walkDepTreeCommonTest(String projectName, String rootId, String[] vulnerableDependencies) throws IOException {
        tempProjectDir = createTempProjectDir(Project.valueOf(projectName).name);
        YarnScanner yarnScanner = new YarnScanner(getProject(), tempProjectDir, executorService, new GraphScanLogic(new NullLog()));

        // Sanity check - make sure the dependency tree is generated.
        DepTree depTree = yarnScanner.buildTree();
        assertNotNull(depTree);
        assertEquals(depTree.rootId(), rootId);

        // Test the walkDepTree method:
        Map<String, DependencyNode> vulnerableDependenciesMap = vulnerableDependenciesMapInit(vulnerableDependencies);

        // Run the method
        List<FileTreeNode> fileTreeNodes = yarnScanner.buildImpactGraph(vulnerableDependenciesMap, depTree);

        // Check there is only one file tree node and it's package.json
        assertEquals(1, fileTreeNodes.size());
        assertEquals("package.json", fileTreeNodes.get(0).getTitle());

        // Check the impact graphs is attached to the package.json node.
        FileTreeNode packageJsonNode = fileTreeNodes.get(0);
        assertEquals(vulnerableDependencies.length, packageJsonNode.getChildren().size());

        // Check the impact graph correctness.
        for (TreeNode treeNode : packageJsonNode.getChildren()) {
            DependencyNode dependencyNode = (DependencyNode) treeNode;
            boolean isIndirect = dependencyNode.isIndirect();
            int impactPathsCount = dependencyNode.getImpactTree().getImpactPathsCount();
            switch (dependencyNode.getComponentId()) {
                // Handle specific cases for different dependencies
                case "axios:1.5.1" -> {
                    assertFalse(isIndirect);
                    assertEquals(3, impactPathsCount);
                }
                case "lodash:4.16.2" -> {
                    if (projectName.equals("MONOREPO")) {
                        assertTrue(isIndirect);
                        assertEquals(1, impactPathsCount);
                    } else {
                        assertFalse(isIndirect);
                        assertEquals(7, impactPathsCount);
                    }
                }
                case "cli-table:0.3.1" -> {
                    assertFalse(isIndirect);
                    assertEquals(1, impactPathsCount);
                }
                case "tough-cookie:2.3.1" -> {
                    assertTrue(isIndirect);
                    assertEquals(2, impactPathsCount);
                }
                default -> fail("Unexpected dependency " + dependencyNode.getComponentId());
            }
        }
    }

    public void testWalkDepTree() throws IOException {
        walkDepTreeCommonTest("EXAMPLE", "example-yarn-package:1.0.0", new String[]{"lodash:4.16.2", "tough-cookie:2.3.1"});
    }

    public void testWalkDepTreeMonorepo() throws IOException {
        walkDepTreeCommonTest("MONOREPO", "example-monorepo:1.0.0", new String[]{"lodash:4.16.2", "axios:1.5.1", "cli-table:0.3.1"});
    }
}
