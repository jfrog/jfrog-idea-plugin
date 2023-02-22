package com.jfrog.ide.idea.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

/**
 * @author michaels
 */

public class InspectionToolsTest extends LightJavaCodeInsightFixtureTestCase {

    public void testConvertFixVersionStringToMinFixVersion() {
        assertEquals("1.0", AbstractInspection.convertFixVersionStringToMinFixVersion("1.0"));
        assertEquals("", AbstractInspection.convertFixVersionStringToMinFixVersion("(,1.0]"));
        assertEquals("", AbstractInspection.convertFixVersionStringToMinFixVersion("(,1.0)"));
        assertEquals("1.0", AbstractInspection.convertFixVersionStringToMinFixVersion("[1.0] "));
        assertEquals("", AbstractInspection.convertFixVersionStringToMinFixVersion("(1.0,)"));
        assertEquals("", AbstractInspection.convertFixVersionStringToMinFixVersion("(1.0, 2.0)"));
        assertEquals("1.0", AbstractInspection.convertFixVersionStringToMinFixVersion("[1.0, 2.0]"));
    }
}