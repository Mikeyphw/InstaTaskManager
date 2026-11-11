package com.mikeyphw.instataskmanager;

/** Deterministic, allocation-free cadence gates for low-overhead samples. */
final class SamplingCadence {
    static final int BASE_INTERVAL_MS = 5000;
    static final int THREAD_EVERY = 4;  // ~20s; CPU averages over scan window
    static final int MEMORY_EVERY = 6;  // ~30s
    static final int PROCESSES_EVERY = 8; // ~40s; UID inventory is expensive
    private SamplingCadence() { }
    static boolean threadScan(int sample) { return sample >= 0 && sample % THREAD_EVERY == 0; }
    static boolean memoryScan(int sample) { return sample >= 0 && sample % MEMORY_EVERY == 0; }
    static boolean processScan(int sample) { return sample >= 0 && sample % PROCESSES_EVERY == 0; }
}
