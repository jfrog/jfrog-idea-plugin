package com.jfrog.ide.idea.inspections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

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
                {":xyz", "xyz"},
                {"a:b", "a:b"},
                {"xyz", "xyz"}
        });
    }

    public GradleInspectionTest(String componentId, String expectedComponentName) {
        this.componentId = componentId;
        this.expectedComponentName = expectedComponentName;
    }

    @Test
    public void testCreateComponentName() {
        assertEquals(expectedComponentName, new GradleGroovyInspection().createComponentName(componentId));
        assertEquals(expectedComponentName, new GradleKotlinInspection().createComponentName(componentId));
    }
}
