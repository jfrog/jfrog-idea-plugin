package com.jfrog.xray.client.services.system;

public interface Version {

    String getVersion();

    String getRevision();

    boolean isAtLeast(String atLeast);
}
