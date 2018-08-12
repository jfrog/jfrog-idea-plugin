package org.jfrog.idea.xray.persistency.types;

import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by romang on 4/11/17.
 */
public class Artifact implements Serializable {

    private static final long serialVersionUID = 1L;

    public GeneralInfo general = new GeneralInfo();
    public Set<Issue> issues = Sets.newHashSet();
    public Set<License> licenses = Sets.newHashSet();

    // Empty constructor for serialization
    public Artifact() {
    }

    public Artifact(com.jfrog.xray.client.services.summary.Artifact artifact) {
        general = new GeneralInfo(artifact.getGeneral());
        issues.addAll(artifact.getIssues()
                .stream()
                .map(Issue::new)
                .collect(Collectors.toSet()));
        licenses.addAll(artifact.getLicenses()
                .stream()
                .map(License::new)
                .collect(Collectors.toSet()));
    }
}


