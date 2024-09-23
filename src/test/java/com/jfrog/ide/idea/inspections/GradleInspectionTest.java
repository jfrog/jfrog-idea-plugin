package com.jfrog.ide.idea.inspections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.jfrog.ide.idea.inspections.GradleInspection.stripVersion;
import static org.junit.Assert.assertEquals;

/**
 * @author yahavi
 **/
@RunWith(Parameterized.class)
public class GradleInspectionTest {
    private final String componentId;
    private final String expectedComponentName;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"a:b:c", "a:b"},
                {"a:b:c:d", "a:b"},
                {"a", "a"},
                {"xyz", "xyz"}
        });
    }

    public GradleInspectionTest(String componentId, String expectedComponentName) {
        this.componentId = componentId;
        this.expectedComponentName = expectedComponentName;
    }

    @Test
    public void testStripVersion() {
        assertEquals(expectedComponentName, stripVersion(componentId));
    }
}
