package com.jfrog.xray.client.services.summary;

import java.io.Serializable;

/**
 * Created by romang on 2/27/17.
 */
public interface General extends Serializable {

    String getName();

    String getPath();

    String getPkgType();

    String getSha256();

    String getComponentId();
}
