package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.License;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicenseImpl implements License {

    private String name;
    private List<String> components = null;
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("more_info_url")
    private List<String> moreInfoUrl;

    @Override
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @Override
    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @Override
    @JsonProperty("components")
    public List<String> getComponents() {
        return components;
    }

    @Override
    @JsonProperty("more_info_url")
    public List<String> moreInfoUrl() {
        return moreInfoUrl;
    }
}