package com.jfrog.xray.client.services.summary;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by romang on 2/27/17.
 */
public interface License extends Serializable {
    @JsonProperty("name")
    String getName();

    @JsonProperty("full_name")
    String getFullName();

    @JsonProperty("components")
    List<String> getComponents();

    @JsonProperty("more_info_url")
    List<String> moreInfoUrl();
}
