package com.mikeyphw.instataskmanager;

import android.app.ActivityManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Only samples the current Instagram process and same-UID process inventory. */
final class ProfilerSampler {
    private static final int MAX_THREADS = 512;
    private static final int MAX_LISTED_THREADS = 15;
    private static final long KB = 1024L;
    private static final long MB = KB * KB;

    static final class HotThread {
        final String name;
        final String hint;
        final double cpuPct;
        HotThread(String name, double cpuPct) {
            this.name = name;
            this.cpuPct = cpuPct;
            this.hint = ThreadStatParser.hint(name);
        }
    }

    static final class Snapshot {
        final double cpuPct, inboundKbPerSec, outboundKbPerSec;
        final long pssKb, rssKb, javaUsedKb, nativeAllocatedKb;
        final int threads;
        final long periodMillis;
        final List<HotThread> topThreads;
        final String processes;

        Snapshot(double cpuPct, double inbound, double outbound,
                 long pssKb, long rssKb, long javaUsedKb, long nativeAllocatedKb,
                 int threads, long periodMillis, List<HotThread> topThreads, String processes) {
            this.cpuPct = cpuPct;
            this.inboundKbPerSec = inbound;
            this.outboundKbPerSec = outbound;
            this.pssKb = pssKb;
            this.rssKb = rssKb;
            this.javaUsedKb = javaUsedKb;
            this.nativeAllocatedKb = nativeAllocatedKb;
            this.threads = threads;
            this.periodMillis = periodMillis;
            this.topThreads = Collections.unmodifiableList(new ArrayList<>(topThreads));
            this.processes = processes;
        }
    }

    private final Context context;
    private final Map<Integer, Long> previousTicks = new HashMap<>();
    private long lastTimeMs = -1, lastProcessCpuMs = -1, lastRx = -1, lastTx = -1;
    private final long ticksPerSecond;
    private int sampleCounter;
    private String cachedProcesses = "Checking same-UID process list…";

    ProfilerSampler(Context context) {
        this.context = context.getApplicationContext();
        long t;
        try { t = Os.sysconf(OsConstants._SC_CLK_TCK); }
        catch (Throwable t1) { t = -1; }
        ticksPerSecond = t > 0 ? t : -1;
    }

    Snapshot capture() {
        long now = SystemClock.elapsedRealtime();
        long cpuMs = Process.getElapsedCpuTime();
        long rx = TrafficStats.getUidRxBytes(Process.myUid());
        long tx = TrafficStats.getUidTxBytes(Process.myUid());
        long period = lastTimeMs < 0 ? -1 : now - lastTimeMs;
        double cpu = percent(cpuMs, lastProcessCpuMs, period);
        double inbound = kbPerSecond(rx, lastRx, period);
        double outbound = kbPerSecond(tx, lastTx, period);

        List<HotThread> hottest = new ArrayList<>();
        File[] rows = new File("/proc/self/task").listFiles();
        int count = 0;
        Map<Integer, Long> current = new HashMap<>();
        if (rows != null) {
            for (File row : rows) {
                if (++count > MAX_THREADS) break;
                int tid;
                try { tid = Integer.parseInt(row.getName()); } catch (NumberFormatException bad) { continue; }
                long ticks = ThreadStatParser.cpuTicks(readFirstLine(new File(row, "stat")));
                if (ticks < 0) continue;
                current.put(tid, ticks);
                Long previous = previousTicks.get(tid);
                if (previous == null || ticksPerSecond <= 0 || period <= 0 || ticks < previous) continue;
                double pct = 100.0 * (ticks - previous) * 1000.0 / (ticksPerSecond * (double) period);
                if (pct < .05 || !Double.isFinite(pct)) continue;
                String threadName = readFirstLine(new File(row, "comm"));
                if (threadName == null || threadName.isEmpty()) threadName = "tid-" + tid;
                hottest.add(new HotThread(sanitize(threadName), pct));
            }
        }
        previousTicks.clear();
        previousTicks.putAll(current);
        hottest.sort((a,b) -> Double.compare(b.cpuPct, a.cpuPct));
        if (hottest.size() > MAX_LISTED_THREADS) hottest.subList(MAX_LISTED_THREADS, hottest.size()).clear();

        Debug.MemoryInfo info = new Debug.MemoryInfo();
        long pss = -1;
        try { Debug.getMemoryInfo(info); pss = info.getTotalPss(); }
        catch (Throwable ignored) { }
        long javaUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / KB;
        long nativeUsed = Debug.getNativeHeapAllocatedSize() / KB;
        long rss = readRssKb();

        // Android Q+ allows memory inspection only for own UID, and it rate-limits queries.
        // Do not use it for process-level CPU attribution, and do not poll every frame.
        if (sampleCounter++ % 4 == 0) cachedProcesses = ownUidProcessList();
        lastTimeMs = now; lastProcessCpuMs = cpuMs; lastRx = rx; lastTx = tx;
        return new Snapshot(cpu, inbound, outbound, pss, rss, javaUsed, nativeUsed,
                            current.size(), period, hottest, cachedProcesses);
    }

    private String ownUidProcessList() {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) return "Unavailable";
            List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
            if (infos == null) return "Not exposed by Android";
            List<ActivityManager.RunningAppProcessInfo> selected = new ArrayList<>();
            for (ActivityManager.RunningAppProcessInfo info : infos) {
                if (info.uid == Process.myUid() && info.processName != null &&
                    info.processName.startsWith("com.instagram.android") && selected.size() < 32) selected.add(info);
            }
            if (selected.isEmpty()) return "None reported (API restrictions may apply)";
            int[] pids = new int[selected.size()];
            for(int i=0; i<selected.size(); i++) pids[i]=selected.get(i).pid;
            Debug.MemoryInfo[] memory = null;
            try { memory = manager.getProcessMemoryInfo(pids); } catch (Throwable ignored) { }
            List<String> lines = new ArrayList<>();
            for (int i=0; i<selected.size(); i++) {
                ActivityManager.RunningAppProcessInfo item = selected.get(i);
                // Android limits other-UID PSS. All pids here must have OUR UID.
                long pss = memory != null && i < memory.length && memory[i] != null
                        ? memory[i].getTotalPss() : -1;
                lines.add(sanitize(item.processName) + " · pid " + item.pid
                        + " · PSS " + (pss > 0 ? mem(pss) : "unavailable"));
            }
            return String.join("\n", lines) + "\n";
        } catch (Throwable denied) { return "Unavailable (restricted by Android)"; }
    }

    static String sanitize(String input) {
        if (input == null) return "?";
        String clean = input.replaceAll("[^A-Za-z0-9_.$# -]", "_");
        return clean.substring(0, Math.min(36, clean.length()));
    }

    static double percent(long current, long previous, long deltaMs) {
        if (previous < 0 || deltaMs <= 0 || current < previous) return Double.NaN;
        return 100.0 * (current - previous) / (double) deltaMs;
    }
    static double kbPerSecond(long current, long previous, long deltaMs) {
        if (current == TrafficStats.UNSUPPORTED || previous < 0 || current < previous || deltaMs <= 0)
            return Double.NaN;
        return (current - previous) * 1000.0 / (1024.0 * deltaMs);
    }
    static String format(double d) {
        return Double.isFinite(d) ? String.format(Locale.US, "%.1f", d) : "—";
    }
    static String mem(long kb) {
        return kb < 0 ? "unavailable" : String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }
    private static long readRssKb() {
        try (BufferedReader in = new BufferedReader(new FileReader("/proc/self/status"))) {
            for (String line; (line = in.readLine()) != null;) {
                if (line.startsWith("VmRSS:")) {
                    String[] parts = line.trim().split("\\s+");
                    return Long.parseLong(parts[1]);
                }
            }
        } catch (Throwable ignored) { }
        return -1;
    }
    private static String readFirstLine(File file) {
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            return in.readLine();
        } catch (IOException | SecurityException ignored) { return null; }
    }
}
