package com.mikeyphw.instataskmanager;

/** Java SE smoke: no Android SDK or physical device required. */
public final class P03CadenceSmoke {
    private static int passed;
    private static void eq(boolean actual, boolean expected, String label) {
        if (actual != expected) throw new AssertionError(label);
        passed++;
    }
    public static void main(String[] args) {
        eq(SamplingCadence.threadScan(0),true,"initial threads");
        eq(SamplingCadence.memoryScan(0),true,"initial memory");
        eq(SamplingCadence.processScan(0),true,"initial process list");
        for(int i=1; i<16; i++) {
            eq(SamplingCadence.threadScan(i),i%4==0,"thread gate " + i);
            eq(SamplingCadence.memoryScan(i),i%6==0,"memory gate " + i);
            eq(SamplingCadence.processScan(i),i%8==0,"process gate " + i);
        }
        eq(SamplingCadence.threadScan(-1),false,"negative thread index");
        eq(SamplingCadence.memoryScan(-1),false,"negative memory index");
        eq(SamplingCadence.processScan(-1),false,"negative process index");
        if (SamplingCadence.BASE_INTERVAL_MS != 5000) throw new AssertionError("cadence interval");
        passed++;
        System.out.println("ITM_P03_SAMPLING_CADENCE_SMOKE_PASS " + passed + "/" + passed);
    }
}
