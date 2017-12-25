package com.jfrog.xray.client.services.summary;

import java.util.Set;

/**
 * Created by romang on 6/1/17.
 */
public interface Components {

    Set<ComponentDetail> getComponentDetails();

    void setComponentDetails(Set<ComponentDetail> componentDetails);

    void addComponent(String componentId, String sha1);
}