package com.mikeyphw.instataskmanager;

/** Compile the ENTIRE Android-backed profiler with tiny API stubs, then assert Snapshot routing. */
public final class P03ProfilerCompileSmoke {
    private static int checks=0;
    private static void ok(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        ProfilerSampler.HotThread hot = new ProfilerSampler.HotThread("dav1d-worker", 42.5);
        ok(hot.cpuPct == 42.5, "hot thread CPU assignment");
        ok("dav1d-worker".equals(hot.name), "hot thread name");
        ProfilerSampler.Snapshot s = new ProfilerSampler.Snapshot(
                123.0, 12.0, 2.0, 0.5, 102400L, 204800L, 128L, 512L,
                20, 5000L, 20000L, java.util.List.of(hot), "inventory");
        ok(s.samplerCpuPct == 0.5, "sampler CPU must belong to Snapshot");
        ok(s.cpuPct == 123.0, "process CPU preserved");
        ok(s.topThreads.get(0) == hot, "thread inventory preserved");
        ok(s.threadPeriodMillis == 20000L, "thread window retained");
        ok(ProfilerSampler.percent(100, 50, 1000) == 5.0, "process CPU calculation");
        ok(ProfilerSampler.kbPerSecond(1024, 0, 1000) == 1.0, "network rate calculation");
        System.out.println("ITM_P03_HF01_FULL_PROFILER_JAVAC_PASS " + checks + "/8");
    }
}
