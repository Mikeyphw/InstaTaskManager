package com.mikeyphw.instataskmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExportReportTest {
    @Test public void containsAllSectionsEvenWhenOnlyOneTabVisible() {
        String report=ExportReport.build(0L,"social","CPU: 12%","RenderThread 4%","4 services","Repost eligibility: STRUCTURE_FOUND");
        assertTrue(report.contains("CPU: 12%"));
        assertTrue(report.contains("RenderThread 4%"));
        assertTrue(report.contains("4 services"));
        assertTrue(report.contains("Repost eligibility: STRUCTURE_FOUND"));
        assertTrue(report.contains("1970-01-01 00:00:00 UTC"));
        assertTrue(report.contains("No network request is made"));
    }
    @Test public void doesNotInventUncollectedResults() {
        String report=ExportReport.build(0L,"overview",null,null,null,null);
        assertEquals(4,report.split("Not measured or not inspected\\.",-1).length-1);
    }
    @Test public void filenameIsSafeAndUniqueAcrossMilliseconds() {
        assertEquals("InstaTaskManager-19700101-000000-000.txt", ExportReport.filename(0L));
        assertNotEquals(ExportReport.filename(0L), ExportReport.filename(1L));
        assertFalse(ExportReport.filename(10L).contains("/"));
    }
    @Test public void stripsControlCharactersAndBoundsUntrustedValues() {
        String input="Valid\u0000\u001b\u202e\r\nValue\t";
        String result=ExportReport.allow(input,128);
        assertEquals("Valid\nValue\t",result);
        String huge="X".repeat(30000);
        String report=ExportReport.build(0L,"overview",huge,null,null,null);
        assertTrue(report.contains("[Section truncated to safe export limit]"));
        assertTrue(report.length()<64000);
    }
}
