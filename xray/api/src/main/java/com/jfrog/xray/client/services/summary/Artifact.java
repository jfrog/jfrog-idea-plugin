package com.jfrog.xray.client.services.summary;

import java.io.Serializable;
import java.util.List;

/**
 * Created by romang on 2/27/17.
 */
public interface Artifact extends Serializable {

    public General getGeneral();

    public List<? extends Issue> getIssues();

    public List<? extends License> getLicenses();
}

