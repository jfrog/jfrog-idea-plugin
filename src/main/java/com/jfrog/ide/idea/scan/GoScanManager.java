package com.jfrog.ide.idea.scan;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.EnvironmentUtil;
import com.jfrog.ide.common.go.GoTreeBuilder;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.projects.GoProject;
import com.jfrog.ide.idea.ui.filters.FilterManagerService;
import com.jfrog.ide.idea.ui.issues.IssuesTree;
import com.jfrog.ide.idea.ui.licenses.LicensesTree;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

/**
 * Created by Bar Belity on 06/02/2020.
 */
public class GoScanManager extends ScanManager {

    private GoTreeBuilder goTreeBuilder;
    private String sumFile;

    /**
     * @param mainProject - Currently opened IntelliJ project. We'll use this project to retrieve project based services
     *                    like {@link FilterManagerService}, {@link LicensesTree} and {@link IssuesTree}.
     * @param project     - Go project {@link GoProject}.
     */
    GoScanManager(Project mainProject, Project project) throws IOException {
        super(mainProject, project, ComponentPrefix.GO);
        getLog().info("Found go project: " + getProjectName());
        goTreeBuilder = new GoTreeBuilder(Utils.getProjectBasePath(project), EnvironmentUtil.getEnvironmentMap(), getLog());
        subscribeGoSumChangeEvents(mainProject);
    }

    @Override
    protected void refreshDependencies(ExternalProjectRefreshCallback cbk, @Nullable Collection<DataNode<LibraryDependencyData>> libraryDependencies) {
        cbk.onSuccess(null);
    }

    @Override
    protected void buildTree(@Nullable DataNode<ProjectData> externalProject) throws IOException {
        setScanResults(goTreeBuilder.buildTree());
    }

    private void subscribeGoSumChangeEvents(Project mainProject) {
        Path goSumPath = Paths.get(project.getBasePath(), "go.sum");
        this.sumFile = goSumPath.toString();

        // Register for file change event of go.sum file.
        mainProject.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    String filePath = event.getPath();
                    if (StringUtils.equals(filePath, sumFile)) {
                        asyncScanAndUpdateResults();
                    }
                }
            }
        });
    }
}
