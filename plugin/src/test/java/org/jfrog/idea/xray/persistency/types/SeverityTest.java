package org.jfrog.idea.xray.persistency.types;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SeverityTest {

    @Test
    public void testFromString() {
        Assert.assertEquals(Severity.fromString("High"), Severity.High);
        Assert.assertEquals(Severity.fromString("Medium"), Severity.Medium);
        Assert.assertEquals(Severity.fromString("Low"), Severity.Low);
        Assert.assertEquals(Severity.fromString("Information"), Severity.Information);
        Assert.assertEquals(Severity.fromString("Unknown"), Severity.Unknown);
        Assert.assertEquals(Severity.fromString("Pending Scan"), Severity.Pending);
        Assert.assertEquals(Severity.fromString("Scanned - No Issues"), Severity.Normal);
        Assert.assertEquals(Severity.fromString("Critical"), Severity.High);
        Assert.assertEquals(Severity.fromString("Major"), Severity.Medium);
        Assert.assertEquals(Severity.fromString("Minor"), Severity.Low);
    }
}