package org.jfrog.idea.xray.persistency.types;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by romang on 4/11/17.
 */
public class Artifact implements Serializable {

    public GeneralInfo general = new GeneralInfo();
    public Set<Issue> issues = new HashSet<>();
    public Set<License> licenses = new HashSet<>();

    public Artifact() {
    }

    public Artifact(com.jfrog.xray.client.services.summary.Artifact artifact) {
        this.general = new GeneralInfo(artifact.getGeneral());
        issues.addAll(Lists.transform(artifact.getIssues(), new Function<com.jfrog.xray.client.services.summary.Issue, Issue>() {
            @Nullable
            @Override
            public Issue apply(@Nullable com.jfrog.xray.client.services.summary.Issue issue) {
                return new Issue(issue);
            }
        }));

        licenses.addAll(Lists.transform(artifact.getLicenses(), new Function<com.jfrog.xray.client.services.summary.License, License>() {
            @Nullable
            @Override
            public License apply(@Nullable com.jfrog.xray.client.services.summary.License license) {
                return new License(license);
            }
        }));
    }
}


