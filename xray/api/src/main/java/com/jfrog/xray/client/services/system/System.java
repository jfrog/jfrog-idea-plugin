package com.jfrog.xray.client.services.system;

import java.io.IOException;

public interface System {

    boolean ping();

    Version version() throws IOException;
}
