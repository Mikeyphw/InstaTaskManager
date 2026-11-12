package com.mikeyphw.instataskmanager;

/**
 * HF13: opt-in, in-memory, short-lease native Repost UI recovery.
 * A/B are manual labels, NOT authenticated account identities. In particular,
 * this is NOT a persistent account-bound enable switch. User must re-arm only
 * while viewing Reels from the Reels TAB and stop before switching accounts.
 * No Repost actions, request handlers, network data, keys or social records
 * are created, read, or persisted. Only the HF12 exact D3r false result is
 * eligible for local UI-state recovery. Native null/true are never changed.
 */
final class P04HF13Recovery {
    static final int LIMIT = 64;
    static final long LEASE_NS = 90_000_000_000L; // explicit re-arm for longer sessions
    interface Clock { long now(); }
    private static Clock clock = System::nanoTime;
    private static boolean enabled;
    private static long expires, epoch=1, leaseEpoch;
    private static int altered, attempts;
    private static String lastReason="Default OFF; Reels tab only (manual confirmation)";

    private P04HF13Recovery() { }
    static synchronized long epoch() {return epoch;}
    static synchronized boolean isEnabled() {
        if (enabled && (clock.now() - expires >= 0 || altered>=LIMIT))
            off("Lease expired or call limit reached");
        return enabled;
    }
    static synchronized void off(String why) {
        enabled=false; expires=0; leaseEpoch=0;
        epoch++; // invalidates already-running frames across disable/account change
        lastReason=why;
    }
    static synchronized void onCaptureTransition() {off("Capture started/stopped or account label changed");}
    static synchronized void onActivityExit() {off("Instagram Activity left foreground");}
    static synchronized void onHookFailure() {off("Exact native data hook unavailable");}

    static synchronized String arm(int cohort, boolean verifiedApk, boolean dataHookReady) {
        off("Re-arm requested");
        if (!verifiedApk) return refusal("Exact Instagram APK/hooks not verified");
        if (!dataHookReady) return refusal("Native D3r hook not confirmed active");
        if (cohort != 0) return refusal("Start A first; only affected manual cohort supported");
        if (!P04ExactGateTrial.isDeoptimized()) return refusal("Exact selector-4 deoptimization unavailable");
        if (!P04ExactGateTrial.eligibleForHF13Recovery())
            return refusal("Fresh Reels-tab A native-data evidence missing/contradicted");
        // Mutual exclusion; HF12's original bounded experiment remains for CI regression.
        P04ExactGateTrial.disarm();
        altered=0;attempts=0;
        leaseEpoch=epoch;
        expires=clock.now()+LEASE_NS;
        enabled=true;
        lastReason="Manual A, Reels TAB only; no verified account identity binding";
        return "ENABLED HF13: native Repost UI only; manual A / Reels tab, " +
                "90 s or 64 changed calls; OFF before account switch. No automatic repost.";
    }
    private static String refusal(String why) {lastReason=why;return "REFUSED HF13: "+why+"; unchanged";}

    /** Caller must have proven the exact D3r int key, selector-4 stack and data. */
    static synchronized Object maybeOverride(long entryEpoch, int cohort, int dataPresence,
                                             boolean nested, Object original) {
        if (!isEnabled()) return original;
        if (entryEpoch!=leaseEpoch || nested || cohort!=0 ||
            dataPresence!=P04BranchInputs.PRESENT || !Boolean.FALSE.equals(original))
            return original;
        attempts++;
        if (!P04ExactGateTrial.isDeoptimized() ||
            !P04ExactGateTrial.eligibleForHF13Recovery()) {
            off("Native eligibility lost; original results restored");
            return original;
        }
        altered++;
        // Final permitted result still changed; next call returns native value.
        if (altered>=LIMIT) {
            enabled=false;leaseEpoch=0;expires=0;epoch++;
            lastReason="64-change guard reached; re-arm manually on Reels tab";
        }
        return Boolean.TRUE;
    }
    static synchronized String status() {
        boolean on=isEnabled();
        long left=on?Math.max(0,(expires-clock.now())/1_000_000_000L):0;
        return (on?"ENABLED (manual A · Reels-tab claim; "+left+"s left)":"OFF")+
            " · altered="+altered+"/"+LIMIT+" · attempts="+attempts+
            " · " + lastReason;
    }
    // Package-only synthetic testing, never used in production paths.
    static synchronized void setClockForTest(Clock value) {off("Test reset");clock=value;}
    static synchronized void restoreClockForTest() {off("Test reset");clock=System::nanoTime;}
}
