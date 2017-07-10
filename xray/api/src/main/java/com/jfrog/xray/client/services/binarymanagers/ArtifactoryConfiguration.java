package com.jfrog.xray.client.services.binarymanagers;

import java.util.List;

/**
 * Created by romang on 2/5/17.
 */
public interface ArtifactoryConfiguration {

    String getArtPass();

    String getArtUser();

    String getBinMgrDesc();

    String getBinMgrId();

    String getBinMgrUrl();

    String getId();

    boolean isLicenseExpired();

    boolean isLicenseValid();

    String getPassword();

    boolean isProxyEnabled();

    List<? extends Repo> getRepos();

    String getUser();

    String getVersion();

    String getXrayPassword();

    String getXraySupportType();

    String getXrayUser();
}
