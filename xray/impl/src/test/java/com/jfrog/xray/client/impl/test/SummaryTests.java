package com.jfrog.xray.client.impl.test;

import com.jfrog.xray.client.impl.ComponentsFactory;
import com.jfrog.xray.client.services.summary.Artifact;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.Error;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Created by romang on 2/27/17.
 */
public class SummaryTests extends XrayTestsBase {

    @Test
    public void testArtifactSummaryNonExistingSha() throws IOException {
        List<String> checksums = new ArrayList<>();
        checksums.add("nonExistingSha");
        SummaryResponse summary = xray.summary().artifact(checksums, null);
        for (Error err : summary.getErrors()) {
            assertEquals(err.getError(), "Artifact doesn't exist or not indexed/cached in Xray");
        }
    }

    @Test
    public void testArtifactSummaryComponent() throws IOException {
        Components components = ComponentsFactory.create();
        components.addComponent("gav://org.acegisecurity:acegi-security:1.0.5", "");
        SummaryResponse summary = xray.summary().component(components);

        assertNull(summary.getErrors());
        assertNotNull(summary.getArtifacts());
        assertNotNull(summary.getArtifacts());
        assertTrue(summary.getArtifacts().size() == 1, "Expecting one Artifact only.");

        for (Artifact artifact : summary.getArtifacts()) {
            assertNotNull(artifact.getGeneral());
            assertNotNull(artifact.getIssues());
            assertNotNull(artifact.getLicenses());
        }
    }

    @Test
    public void testArtifactSummaryNonExistingPath() throws IOException {
        List<String> paths = new ArrayList<>();
        paths.add("non/existing/path");
        SummaryResponse summary = xray.summary().artifact(null, paths);
        for (Error err : summary.getErrors()) {
            assertEquals(err.getError(), "Artifact doesn't exist or not indexed/cached in Xray");
        }
    }

    @Test
    public void testArtifactSummary() throws IOException {
        SummaryResponse summary = xray.summary().artifact(null, null);
        assertNull(summary.getArtifacts());
        assertNull(summary.getErrors());
    }
}
