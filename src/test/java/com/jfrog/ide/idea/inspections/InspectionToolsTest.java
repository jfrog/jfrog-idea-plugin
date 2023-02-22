package com.jfrog.ide.idea.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author michaels
 */

public class InspectionToolsTest extends LightJavaCodeInsightFixtureTestCase {

    @DataProvider
    private Object[][] goVersionProvider() {
        return new Object[][]{
                {"1.0", "1.0"},
                {"", "(,1.0]"},
                {"", "(,1.0)"},
                {"1.0", "[1.0]"},
                {"", "(1.0,)"},
                {"", "(1.0, 2.0)"},
                {"1.0", "[1.0, 2.0]"},
        };
    }

    @Test(dataProvider = "goVersionProvider")
    public void testConvertFixVersionStringToMinFixVersion(String versionOutput, String expectedVersion) {
        assertEquals(expectedVersion, AbstractInspection.convertFixVersionStringToMinFixVersion(versionOutput));
    }
}