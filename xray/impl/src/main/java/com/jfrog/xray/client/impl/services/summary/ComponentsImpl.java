package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import com.jfrog.xray.client.services.summary.ComponentDetail;
import com.jfrog.xray.client.services.summary.Components;

import java.util.Set;


/**
 * Created by romang on 6/1/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComponentsImpl implements Components {

    @JsonProperty("component_details")
    private Set<ComponentDetail> componentDetails;

    public ComponentsImpl() {
        componentDetails = Sets.newHashSet();
    }

    public ComponentsImpl(Set<ComponentDetail> componentDetails) {
        this.componentDetails = componentDetails;
    }

    @JsonProperty("component_details")
    @Override
    public Set<ComponentDetail> getComponentDetails() {
        return componentDetails;
    }

    @JsonProperty("component_details")
    @Override
    public void setComponentDetails(Set<ComponentDetail> componentDetails) {
        this.componentDetails = componentDetails;
    }

    @Override
    public void addComponent(String componentId, String sha1) {
        if (componentDetails != null) {
            componentDetails.add(new ComponentDetailImpl(componentId, sha1));
        }
    }
}