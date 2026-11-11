package com.mikeyphw.instataskmanager;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThreadStatParserTest {
    private String row(String name, String u, String s) {
        StringBuilder b=new StringBuilder("123 (").append(name).append(") S");
        // fields 4 through 13: 10 fields following state
        for(int i=0;i<10;i++) b.append(" 0");
        b.append(" ").append(u).append(" ").append(s).append(" 0 0");
        return b.toString();
    }
    @Test public void parseBasic() { assertEquals(110L,ThreadStatParser.cpuTicks(row("Background", "100", "10"))); }
    @Test public void parseParenthesisInName() { assertEquals(12L,ThreadStatParser.cpuTicks(row("Render) Worker (IG", "5", "7"))); }
    @Test public void malformedRefused() {
        assertEquals(-1L,ThreadStatParser.cpuTicks(null));
        assertEquals(-1L,ThreadStatParser.cpuTicks("1 (missing)"));
        assertEquals(-1L,ThreadStatParser.cpuTicks(row("bad", "-1", "1")));
        assertEquals(-1L,ThreadStatParser.cpuTicks(row("overflow", "9223372036854775807", "1")));
        assertEquals(-1L,ThreadStatParser.cpuTicks(row("bad", "someText", "1")));
    }
    @Test public void hintsAreNotAuthoritative() {
        assertEquals("Media playback?",ThreadStatParser.hint("ExoPlayer"));
        assertEquals("Unclassified",ThreadStatParser.hint("Thread-17"));
        assertTrue(ThreadStatParser.hint("RenderThread").endsWith("?"));
    }
}
