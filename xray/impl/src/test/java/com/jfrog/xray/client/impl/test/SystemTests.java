package com.jfrog.xray.client.impl.test;

import com.jfrog.xray.client.services.system.Version;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by romang on 2/2/17.
 */
public class SystemTests extends XrayTestsBase {

    @Test
    public void testPing() {
        assertTrue(xray.system().ping());
    }

    @Test
    public void testVersion() throws IOException {
        Version version = xray.system().version();
        assertNotNull(version.getVersion());
        assertNotNull(version.getRevision());
    }
}
