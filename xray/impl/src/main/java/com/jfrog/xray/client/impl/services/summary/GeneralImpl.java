package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.General;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneralImpl implements General {

    private String name;
    private String path;
    private String sha256;
    @JsonProperty("pkg_type")
    private String pkgType;
    @JsonProperty("component_id")
    private String componentId;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("pkg_type")
    public String getPkgType() {
        return pkgType;
    }

    @JsonProperty("sha256")
    public String getSha256() {
        return sha256;
    }

    @JsonProperty("component_id")
    public String getComponentId() {
        return componentId;
    }
}