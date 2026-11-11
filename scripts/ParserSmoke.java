package com.mikeyphw.instataskmanager;
final class ParserSmoke {
    private static String stat(String name, String u, String s) {
        StringBuilder b = new StringBuilder("100 (").append(name).append(") R");
        for (int i=0;i<10;i++) b.append(" 0");
        return b.append(" ").append(u).append(" ").append(s).append(" 0 0").toString();
    }
    static void yes(boolean b) { if(!b) throw new AssertionError("proc parser mismatch"); }
    public static void main(String[] args) {
        yes(ThreadStatParser.cpuTicks(stat("Render) Thread", "5", "7")) == 12);
        yes(ThreadStatParser.cpuTicks(stat("Normal", "0", "0")) == 0);
        yes(ThreadStatParser.cpuTicks(stat("Bad", "-2", "0")) == -1);
        yes(ThreadStatParser.cpuTicks(stat("Bad", "9223372036854775807", "1")) == -1);
        yes(ThreadStatParser.cpuTicks("not proc stat") == -1);
        yes(ThreadStatParser.hint("ExoPlayer").equals("Media playback?"));
        yes(ThreadStatParser.hint("RenderThread").endsWith("?"));
        yes(ThreadStatParser.hint("Worker-1").equals("Unclassified"));
        System.out.println("ITM_P00_PURE_JAVA_SMOKE_PASS 8/8");
    }
}
