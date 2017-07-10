package org.jfrog.idea.xray.persistency;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.xray.persistency.types.Artifact;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by romang on 4/10/17.
 */

@State(name = "XrayScanCache", storages = {@Storage(file = "XrayScanCache.xml")})
public final class ScanCache implements PersistentStateComponent<ScanCache> {

    public Map<String, Artifact> artifacts = new HashMap<>();
    public Map<String, String> lastUpdated = new HashMap<>();

    public static ScanCache getInstance(Project project) {
        return ServiceManager.getService(project, ScanCache.class);
    }

    @Nullable
    @Override
    public ScanCache getState() {
        return this;
    }

    @Override
    public void loadState(ScanCache state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public LocalDateTime getLastUpdateTime(String componentId) {
        if (lastUpdated.get(componentId) == null) {
            return null;
        }
        return LocalDateTime.parse(lastUpdated.get(componentId));
    }

    public Artifact getArtifact(String componentId) {
        return artifacts.get(componentId);
    }

    public void updateArtifact(String componentId, com.jfrog.xray.client.services.summary.Artifact artifact) {
        artifacts.put(componentId, new Artifact(artifact));
    }

    public void setLastUpdated(String componentId) {
        lastUpdated.put(componentId, LocalDateTime.now().toString());
    }
}
