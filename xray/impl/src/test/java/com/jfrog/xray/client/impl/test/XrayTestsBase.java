package com.jfrog.xray.client.impl.test;

import com.jfrog.xray.client.Xray;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.jfrog.xray.client.impl.XrayClient.create;
import static org.junit.Assert.fail;


/**
 * Created by romang on 2/2/17.
 */
public class XrayTestsBase {
    private static final String CLIENTTESTS_XRAY_ENV_VAR_PREFIX = "CLIENTTESTS_XRAY_";
    private static final String CLIENTTESTS_XRAY_PROPERTIES_PREFIX = "clienttests.xray.";

    private String url;
    private String username;
    private String password;
    protected Xray xray;

    @BeforeClass
    public void init() throws IOException {

        Properties props = new Properties();
        // This file is not in GitHub. Create your own in src/test/resources.
        InputStream inputStream = this.getClass().getResourceAsStream("/xray-client.properties");
        if (inputStream != null) {
            props.load(inputStream);
        }

        url = readParam(props, "url");
        if (!url.endsWith("/")) {
            url += "/";
        }
        username = readParam(props, "username");
        password = readParam(props, "password");

        xray = create(url, username, password);
    }

    private String readParam(Properties props, String paramName) {
        String paramValue = null;
        if (props.size() > 0) {
            paramValue = props.getProperty(CLIENTTESTS_XRAY_PROPERTIES_PREFIX + paramName);
        }
        if (paramValue == null) {
            paramValue = System.getProperty(CLIENTTESTS_XRAY_PROPERTIES_PREFIX + paramName);
        }
        if (paramValue == null) {
            paramValue = System.getenv(CLIENTTESTS_XRAY_ENV_VAR_PREFIX + paramName.toUpperCase());
        }
        if (paramValue == null) {
            failInit();
        }
        return paramValue;
    }

    private void failInit() {
        String message =
                new StringBuilder("Failed to load test Artifactory instance credentials. ")
                        .append("Looking for System properties '")
                        .append(CLIENTTESTS_XRAY_PROPERTIES_PREFIX)
                        .append("url', ")
                        .append(CLIENTTESTS_XRAY_PROPERTIES_PREFIX)
                        .append("username' and ")
                        .append(CLIENTTESTS_XRAY_PROPERTIES_PREFIX)
                        .append("password' or a properties file with those properties in classpath ")
                        .append("or Environment variables '")
                        .append(CLIENTTESTS_XRAY_ENV_VAR_PREFIX).append("URL', ")
                        .append(CLIENTTESTS_XRAY_ENV_VAR_PREFIX).append("USERNAME' and ")
                        .append(CLIENTTESTS_XRAY_ENV_VAR_PREFIX).append("PASSWORD'").toString();

        fail(message);
    }

    @AfterClass
    public void clean() {
        xray.close();
    }
}
