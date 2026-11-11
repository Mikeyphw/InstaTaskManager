package com.mikeyphw.instataskmanager;

/** Pure-Java /proc/<pid>/task/<tid>/stat parser. Returns -1 for malformed rows. */
public final class ThreadStatParser {
    private ThreadStatParser() { }
    public static long cpuTicks(String stat) {
        if (stat == null) return -1;
        int open = stat.indexOf('('), close = stat.lastIndexOf(')');
        if (open < 1 || close <= open || close + 2 >= stat.length() || stat.charAt(close+1) != ' ') return -1;
        // The rest begins at kernel field 3: state, ppid, pgrp ... utime(14), stime(15).
        String[] cols = stat.substring(close + 2).trim().split("\\s+");
        if (cols.length < 13 || cols[0].length() != 1) return -1;
        try {
            long user = Long.parseLong(cols[11]), system = Long.parseLong(cols[12]);
            if (user < 0 || system < 0 || user > Long.MAX_VALUE - system) return -1;
            return user + system;
        } catch (NumberFormatException ignored) { return -1; }
    }
    public static String hint(String threadName) {
        if (threadName == null) return "Unclassified";
        String n = threadName.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("render") || n.contains("hwui") || n.contains("egl") || n.contains("gpu")) return "Rendering?";
        if (n.contains("codec") || n.contains("video") || n.contains("audio") || n.contains("player")) return "Media playback?";
        if (n.contains("okhttp") || n.contains("http") || n.contains("network") || n.contains("cronet")) return "Networking?";
        if (n.contains("image") || n.contains("fresco") || n.contains("decode")) return "Images?";
        if (n.contains("analytics") || n.contains("logging") || n.contains("telemetry")) return "Telemetry?";
        if (n.contains("binder")) return "Android IPC?";
        return "Unclassified";
    }
}
