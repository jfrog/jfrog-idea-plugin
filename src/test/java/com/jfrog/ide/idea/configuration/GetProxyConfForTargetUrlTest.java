package com.jfrog.ide.idea.configuration;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.net.HttpConfigurable;
import com.jfrog.ide.common.configuration.ServerConfig;
import org.jfrog.build.client.ProxyConfiguration;

import java.io.File;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * @author yahavi
 */
public class GetProxyConfForTargetUrlTest extends LightJavaCodeInsightFixtureTestCase {

    private final ServerConfig serverConfig = new ServerConfigImpl();
    private HttpConfigurable httpConfigurable;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Disable all proxies
        httpConfigurable = HttpConfigurable.getInstance();
        httpConfigurable.USE_HTTP_PROXY = false;
        httpConfigurable.USE_PROXY_PAC = false;

        // Set PAC URL
        URL pacUrl = getClass().getClassLoader().getResource("proxy/proxy.pac");
        assertNotNull(pacUrl);
        File pacFile = new File(pacUrl.getFile());
        httpConfigurable.PAC_URL = pacFile.getAbsolutePath();
        httpConfigurable.USE_PAC_URL = true;
    }

    /**
     * Check that we get null proxy configuration if proxy is not defined.
     */
    public void testProxyNotConfigured() {
        ProxyConfiguration proxyConfig = serverConfig.getProxyConfForTargetUrl("https://1.2.3.4");
        assertNull(proxyConfig);
    }

    /**
     * Check manual proxy configuration.
     */
    public void testManuallyConfiguredProxy() {
        // Set manual proxy configuration
        httpConfigurable.USE_HTTP_PROXY = true;
        httpConfigurable.PROXY_HOST = "proxyHost.org";
        httpConfigurable.PROXY_PORT = 8888;
        httpConfigurable.PROXY_AUTHENTICATION = true;
        httpConfigurable.setProxyLogin("admin");
        httpConfigurable.setPlainProxyPassword("password");

        // Get proxy config for https://1.2.3.4
        ProxyConfiguration proxyConfig = serverConfig.getProxyConfForTargetUrl("https://1.2.3.4");
        assertNotNull(proxyConfig);

        // Check proxy config
        assertEquals("proxyHost.org", proxyConfig.host);
        assertEquals(8888, proxyConfig.port);
        assertEquals("admin", proxyConfig.username);
        assertEquals("password", proxyConfig.password);
    }

    /**
     * Test proxy configuration using PAC file.
     * The PAC file is configured to return "proxyPacHost.org:8888" for "https://1.2.3.4" URL.
     */
    public void testPacConfiguredProxy() {
        // Set PAC proxy configuration
        httpConfigurable.USE_PROXY_PAC = true;
        PasswordAuthentication passwordAuthentication = new PasswordAuthentication("admin", "password".toCharArray());
        httpConfigurable.putGenericPassword("proxyPacHost.org", 8888, passwordAuthentication, true);

        // Get proxy config for https://1.2.3.4
        ProxyConfiguration proxyConfig = serverConfig.getProxyConfForTargetUrl("https://1.2.3.4");
        assertNotNull(proxyConfig);

        // Check proxy config
        assertEquals("proxyPacHost.org", proxyConfig.host);
        assertEquals(8888, proxyConfig.port);
        assertEquals("admin", proxyConfig.username);
        assertEquals("password", proxyConfig.password);
    }

    /**
     * Test proxy configuration using PAC file.
     * The PAC file is configured to return "DIRECT" for "https://1.2.3.5" URL.
     */
    public void testPacConfiguredNoProxy() {
        // Set PAC proxy configuration
        httpConfigurable.USE_PROXY_PAC = true;

        // Assert no proxy config for https://1.2.3.5
        ProxyConfiguration proxyConfig = serverConfig.getProxyConfForTargetUrl("https://1.2.3.5");
        assertNull(proxyConfig);
    }
}
