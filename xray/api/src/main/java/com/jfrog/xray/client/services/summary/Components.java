package com.jfrog.xray.client.services.summary;

import java.util.List;

/**
 * Created by romang on 6/1/17.
 */
public interface Components {

    List<ComponentDetail> getComponentDetails();

    void setComponentDetails(List<ComponentDetail> componentDetails);

    void addComponent(String componentId, String sha1);
}