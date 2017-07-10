
package com.jfrog.xray.client.impl.services.binarymanagers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.binarymanagers.Repo;

public class RepoImpl implements Repo {

    private String name;
    private String pkgType;
    private String type;

    public RepoImpl() {
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("pkgType")
    public String getPkgType() {
        return pkgType;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }
}
