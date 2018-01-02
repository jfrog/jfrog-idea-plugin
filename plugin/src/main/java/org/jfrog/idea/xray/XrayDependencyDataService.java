package org.jfrog.idea.xray;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jfrog.idea.Events;
import org.jfrog.idea.configuration.GlobalSettings;
import org.jfrog.idea.xray.scan.ScanManager;

import java.util.Collection;

/**
 * Created by Yahav Itzhak on 9 Nov 2017.
 */
@Order(ExternalSystemConstants.UNORDERED)
public class XrayDependencyDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {

    @NotNull
    @Override
    public Key<LibraryDependencyData> getTargetDataKey() {
        return ProjectKeys.LIBRARY_DEPENDENCY;
    }

    /**
     * This function is called after change in the build.gradle file or refresh gradle dependencies call.
     * @param toImport the project dependencies
     * @param projectData the project data
     * @param project the current project
     * @param modelsProvider contains the project modules
     */
    @Override
    public void importData(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {
        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        ScanManager scanManager = ScanManagerFactory.getScanManager(project);
        if (scanManager == null) {
            ScanManagerFactory scanManagerFactory = ServiceManager.getService(project, ScanManagerFactory.class);
            scanManagerFactory.initScanManager(project);
            scanManager = ScanManagerFactory.getScanManager(project);
            if (scanManager == null) {
                return;
            }
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(Events.ON_IDEA_FRAMEWORK_CHANGE).update();
        }
        if (GlobalSettings.getInstance().isCredentialsSet()) {
            scanManager.asyncScanAndUpdateResults(true, toImport);
        }
    }
}