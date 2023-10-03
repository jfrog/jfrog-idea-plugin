package org.owasp.webgoat;

import org.dummy.insecure.framework.VulnerableTaskHolder;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.lessons.deserialization.SerializationHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DeserializationIntegrationTest extends IntegrationTest {

    private static String OS = System.getProperty("os.name").toLowerCase();

    @Test
    public void runTests() throws IOException {
        startLesson("InsecureDeserialization");

        Map<String, Object> params = new HashMap<>();
        params.clear();

        if (OS.indexOf("win") > -1) {
            params.put("token", SerializationHelper.toString(new VulnerableTaskHolder("wait", "ping localhost -n 5")));
        } else {
            params.put("token", SerializationHelper.toString(new VulnerableTaskHolder("wait", "sleep 5")));
        }
        checkAssignment(url("/WebGoat/InsecureDeserialization/task"), params, true);

        checkResults("/InsecureDeserialization/");

    }


}
