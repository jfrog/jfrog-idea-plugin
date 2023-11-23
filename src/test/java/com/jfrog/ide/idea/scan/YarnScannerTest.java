package com.jfrog.ide.idea.scan;

import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.util.ConcurrencyUtil;
import com.jfrog.ide.common.deptree.DepTree;
import com.jfrog.ide.common.nodes.DependencyNode;
import com.jfrog.ide.common.nodes.FileTreeNode;
import com.jfrog.ide.common.scan.GraphScanLogic;
import org.apache.commons.io.FileUtils;
import org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil;
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
        tempProjectDir = createTempProjectDir("exampleYarnPackage");
        YarnScanner yarnScanner = new YarnScanner(getProject(), tempProjectDir, executorService, new GraphScanLogic(new NullLog()));
        Set<String> packages = new HashSet<>(Arrays.asList("package1:1.0.123", "package2:2.0.9", "package1:1.0.123", "package2:2.1.7"));
        Map<String, Set<String>> packageNameToVersions = yarnScanner.getPackageNameToVersionsMap(packages);

        assertEquals(2, packageNameToVersions.size());
        assertTrue(packageNameToVersions.containsKey("package1"));
        assertTrue(packageNameToVersions.containsKey("package2"));
        assertFalse(packageNameToVersions.containsKey("package3"));

        assertEquals(new HashSet<>(List.of("1.0.123")), packageNameToVersions.get("package1"));
        assertEquals(new HashSet<>(Arrays.asList("2.0.9", "2.1.7")), packageNameToVersions.get("package2"));
    }

    private Map<String, DependencyNode> vulnerableDependenciesMapInit(String[] vulnerableDependencies) {
        Map<String, DependencyNode> vulnerableDependenciesMap = new HashMap<>();
        for (String vulnerableDependency : vulnerableDependencies) {
            DependencyNode dependencyNode = new DependencyNode();
            dependencyNode.componentId(vulnerableDependency);
            // Insert a dummy node and remove it to init the children vector.
            dependencyNode.insert(new DependencyNode(), 0);
            dependencyNode.remove(0);
            vulnerableDependenciesMap.put(vulnerableDependency, dependencyNode);
        }
        return vulnerableDependenciesMap;
    }
    public void testWalkDepTree() throws IOException {
        tempProjectDir = createTempProjectDir("exampleYarnPackage");
        GradleProjectImportUtil.linkAndRefreshGradleProject(tempProjectDir, getProject());
        YarnScanner yarnScanner = new YarnScanner(getProject(), tempProjectDir, executorService, new GraphScanLogic(new NullLog()));

        // Sanity check - make sure the dependency tree is generated.
        DepTree depTree = yarnScanner.buildTree();
        assertNotNull(depTree);
        assertEquals(depTree.getRootId(), "example-yarn-package:1.0.0");

        // Test the walkDepTree method:
        // Init params
        String[] vulnerableDependencies = {"lodash:4.16.2", "tough-cookie:2.3.1"};
        Map<String, DependencyNode> vulnerableDependenciesMap = vulnerableDependenciesMapInit(vulnerableDependencies);

        // Run the method
        List<FileTreeNode> fileTreeNodes = yarnScanner.walkDepTree(vulnerableDependenciesMap, depTree);
        // Check there is only one file tree node and it's package.json
        assertEquals(1, fileTreeNodes.size());
        assertEquals("package.json", fileTreeNodes.get(0).getTitle());

        // Check the impact graphs is attached to the package.json node.
        FileTreeNode packageJsonNode = fileTreeNodes.get(0);
        assertEquals(vulnerableDependencies.length, packageJsonNode.getChildren().size());

        // Check the impact graph correctness.
        DependencyNode toughCookieDep = (DependencyNode) packageJsonNode.getChildren().get(0);
        DependencyNode lodashDep = (DependencyNode) packageJsonNode.getChildren().get(1);
        if (lodashDep.getComponentId().equals("tough-cookie:2.3.1")) {
            DependencyNode temp = lodashDep;
            lodashDep = toughCookieDep;
            toughCookieDep = temp;
        }

        // lodash:4.16.2 tests
        assertEquals("lodash:4.16.2", lodashDep.getComponentId());
        assertFalse(lodashDep.isIndirect());
        assertEquals(7, lodashDep.getImpactTree().getImpactPathsCount());

        // tough-cookie:2.3.1 tests
        assertEquals("tough-cookie:2.3.1", toughCookieDep.getComponentId());
        assertTrue(toughCookieDep.isIndirect());
        assertEquals(2, toughCookieDep.getImpactTree().getImpactPathsCount());
    }

    public void testWalkDepTreeMonorepo() throws IOException {
        tempProjectDir = createTempProjectDir("exampleYarnMonorepo");
        GradleProjectImportUtil.linkAndRefreshGradleProject(tempProjectDir, getProject());
        YarnScanner yarnScanner = new YarnScanner(getProject(), tempProjectDir, executorService, new GraphScanLogic(new NullLog()));

        // Sanity check - make sure the dependency tree is generated.
        DepTree depTree = yarnScanner.buildTree();
        assertNotNull(depTree);
        assertEquals(depTree.getRootId(), "example-monorepo:1.0.0");

        // Test the walkDepTree method:
        // Init params
        String[] vulnerableDependencies = {"lodash:4.16.2", "axios:1.5.1", "cli-table:0.3.1"};
        Map<String, DependencyNode> vulnerableDependenciesMap = vulnerableDependenciesMapInit(vulnerableDependencies);

        // Run the method
        List<FileTreeNode> fileTreeNodes = yarnScanner.walkDepTree(vulnerableDependenciesMap, depTree);
        // Check there is only one file tree node and it's package.json
        assertEquals(1, fileTreeNodes.size());
        assertEquals("package.json", fileTreeNodes.get(0).getTitle());

        // Check the impact graphs is attached to the package.json node.
        FileTreeNode packageJsonNode = fileTreeNodes.get(0);
        assertEquals(vulnerableDependencies.length, packageJsonNode.getChildren().size());

        // Check the impact graph correctness.
        boolean isIndirect = false;
        int actualImpactPathsCount = 0;
        for (TreeNode treeNode : packageJsonNode.getChildren()) {
            DependencyNode dependencyNode = (DependencyNode) treeNode;
            switch (dependencyNode.getComponentId()) {
                case "axios:1.5.1" -> {
                    isIndirect = false;
                    actualImpactPathsCount = 3;
                }
                case "lodash:4.16.2" -> {
                    isIndirect = true;
                    actualImpactPathsCount = 1;
                }
                case "cli-table:0.3.1" -> {
                    isIndirect = false;
                    actualImpactPathsCount = 1;
                }
                default -> fail("Unexpected dependency " + dependencyNode.getComponentId());
            }
            assertEquals(isIndirect, dependencyNode.isIndirect());
            assertEquals(actualImpactPathsCount, dependencyNode.getImpactTree().getImpactPathsCount());
        }
    }
}
