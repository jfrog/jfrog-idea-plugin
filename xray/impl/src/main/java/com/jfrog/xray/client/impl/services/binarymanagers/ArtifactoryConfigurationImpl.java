
package com.jfrog.xray.client.impl.services.binarymanagers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.binarymanagers.ArtifactoryConfiguration;

import java.util.List;

public class ArtifactoryConfigurationImpl implements ArtifactoryConfiguration {

    private String artPass;
    private String artUser;
    private String binMgrDesc;
    private String binMgrId;
    private String binMgrUrl;
    private String id;
    private String password;
    private List<RepoImpl> repos = null;
    private String user;
    private String version;
    private String xrayPassword;
    private String xraySupportType;
    private String xrayUser;
    @JsonProperty("proxy_enabled")
    private boolean proxyEnabled;
    @JsonProperty("license_valid")
    private boolean licenseValid;
    @JsonProperty("license_expired")
    private boolean licenseExpired;

    public ArtifactoryConfigurationImpl() {
    }

    @JsonProperty("artPass")
    public String getArtPass() {
        return artPass;
    }

    @JsonProperty("artUser")
    public String getArtUser() {
        return artUser;
    }

    @JsonProperty("binMgrDesc")
    public String getBinMgrDesc() {
        return binMgrDesc;
    }

    @JsonProperty("binMgrId")
    public String getBinMgrId() {
        return binMgrId;
    }

    @JsonProperty("binMgrUrl")
    public String getBinMgrUrl() {
        return binMgrUrl;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("license_expired")
    public boolean isLicenseExpired() {
        return licenseExpired;
    }

    @JsonProperty("license_valid")
    public boolean isLicenseValid() {
        return licenseValid;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonProperty("proxy_enabled")
    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    @JsonProperty("repos")
    public List<RepoImpl> getRepos() {
        return repos;
    }

    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("xrayPassword")
    public String getXrayPassword() {
        return xrayPassword;
    }

    @JsonProperty("xraySupportType")
    public String getXraySupportType() {
        return xraySupportType;
    }

    @JsonProperty("xrayUser")
    public String getXrayUser() {
        return xrayUser;
    }
}
