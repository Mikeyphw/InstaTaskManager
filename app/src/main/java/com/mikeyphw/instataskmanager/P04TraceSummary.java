package com.mikeyphw.instataskmanager;

import java.util.Locale;

/** Bounded counts only; neither return objects nor account/media identifiers are retained. */
final class P04TraceSummary {
    static final int LIMIT_PER_GATE = 256;
    static final int[] SELECTORS = {4, 11, 12};
    static final String[] LABELS = {
            "Repost UI/eligibility selector 4",
            "Repost UFI selector 11",
            "Repost count/clickable selector 12"
    };
    static final class Count {
        int observed, trueValues, falseValues, nullValues, objectValues, dropped;
        String lastObjectClass = "none"; // class name only, never Object.toString()
        void record(Object result) {
            if (observed >= LIMIT_PER_GATE) { dropped++; return; }
            observed++;
            if (result == null) nullValues++;
            else if (result instanceof Boolean) {
                if ((Boolean) result) trueValues++;
                else falseValues++;
            } else {
                objectValues++;
                // Only code metadata, not content or Object.toString().
                String type = result.getClass().getName();
                lastObjectClass = type.length() > 80 ? type.substring(0,80) : type;
            }
        }
    }
    private final Count[][] counts = new Count[2][SELECTORS.length];
    private final String[] uiSamples = {"Not checked", "Not checked"};
    private int active = -1;
    P04TraceSummary() { clear(); }
    synchronized void clear() {
        for (int a=0;a<2;a++) {
            for (int g=0;g<SELECTORS.length;g++) counts[a][g] = new Count();
            uiSamples[a] = "Not checked";
        }
        active=-1;
    }
    synchronized int start(int accountIndex) {
        if (accountIndex<0 || accountIndex>1) return -1;
        active=accountIndex;
        return active;
    }
    synchronized void stop() {active=-1;}
    synchronized boolean activeA() {return active==0;}
    /** Strong scalar A/B signal, NOT proof of permission or shared rollout. */
    synchronized boolean booleanGapReady() {
        Count a=counts[0][0],b=counts[1][0];
        return a.observed>=3 && b.observed>=3 && a.falseValues==a.observed
                && b.trueValues==b.observed && a.dropped==0 && b.dropped==0;
    }
    synchronized boolean recording() {return active>=0;}
    synchronized void record(int selector, Object result) {
        if (active < 0) return;
        for (int i=0;i<SELECTORS.length;i++)
            if (SELECTORS[i]==selector) {counts[active][i].record(result);return;}
    }
    synchronized void uiCheck(String sanitizedCounts) {
        if (active>=0) uiSamples[active]=sanitizedCounts;
    }
    synchronized String render() {
        StringBuilder b = new StringBuilder();
        b.append("P04 SESSION (manual labels; not account identity): ")
         .append(active < 0 ? "STOPPED" : (active==0?"A":"B")+" recording")
         .append("\n");
        for (int a=0;a<2;a++) {
            b.append("\nACCOUNT ").append(a==0?'A':'B').append(" (manually selected)\n");
            for (int i=0;i<SELECTORS.length;i++) {
                Count c=counts[a][i];
                b.append(LABELS[i]).append(": samples=").append(c.observed)
                 .append(" true=").append(c.trueValues)
                 .append(" false=").append(c.falseValues)
                 .append(" null=").append(c.nullValues)
                 .append(" object=").append(c.objectValues)
                 .append(" capped_extra=").append(c.dropped)
                 .append(" type=").append(c.lastObjectClass).append("\n");
            }
            b.append("Visible UI label counts: ").append(uiSamples[a]).append("\n");
        }
        b.append("Interpretation: selection-specific method outcomes are NOT proof of account eligibility.\n")
         .append("Non-Boolean result objects are NOT coerced, mutated, or printed.\n")
         .append("Captures are volatile and disappear when the Instagram process exits.\n");
        return b.toString();
    }
}
