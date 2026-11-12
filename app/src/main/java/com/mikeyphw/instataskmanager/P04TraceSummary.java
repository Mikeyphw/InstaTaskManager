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
        int singletonVariant, payloadVariant, unknownVariant, invalidShape;
        int intZero, intPositive, intNegative;
        int flag06True, flag06False, flag07True, flag07False;
        int referenceFieldsPresent, stringFieldsPresent;
        String lastObjectClass = "none"; // class name only, never Object.toString()
        final java.util.Map<String,Integer> classCounts = new java.util.TreeMap<>();
        int unlistedClasses;
        void record(int selector,Object result) {
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
                // Capture code CLASS metadata per result, not only last class.
                // Never stringify an Instagram return object. Cap novel classes.
                if(classCounts.containsKey(lastObjectClass))
                    classCounts.put(lastObjectClass,classCounts.get(lastObjectClass)+1);
                else if(classCounts.size()<6 && lastObjectClass.startsWith("X."))
                    classCounts.put(lastObjectClass,1);
                else unlistedClasses++;
                P04VariantShape.Observation shape=P04VariantShape.inspect(selector,result);
                if (shape.variant==P04VariantShape.Observation.SINGLETON) singletonVariant++;
                else if (shape.variant==P04VariantShape.Observation.PAYLOAD) {
                    payloadVariant++;
                    if(shape.integerSign==0)intZero++;
                    else if(shape.integerSign>0)intPositive++;
                    else intNegative++;
                    if(shape.hasUiFlags) {
                        if(shape.uiFlag06)flag06True++;else flag06False++;
                        if(shape.uiFlag07)flag07True++;else flag07False++;
                    }
                    referenceFieldsPresent+=shape.presentReferences;
                    stringFieldsPresent+=shape.presentStrings;
                }
                else if (shape.variant==P04VariantShape.Observation.SHAPE_MISMATCH) invalidShape++;
                else unknownVariant++;
            }
        }
    }
    private final Count[][] counts = new Count[2][SELECTORS.length];
    private final String[] uiSamples = {"Not checked", "Not checked"};
    private int active = -1;
    private int lastActive = -1;
    private long startedNanos;
    private long lastElapsedSeconds;
    P04TraceSummary() { clear(); }
    synchronized void clear() {
        for (int a=0;a<2;a++) {
            for (int g=0;g<SELECTORS.length;g++) counts[a][g] = new Count();
            uiSamples[a] = "Not checked";
        }
        active=-1;
        lastActive=-1;
        startedNanos=0;
        lastElapsedSeconds=0;
    }
    synchronized int start(int accountIndex) {
        if (accountIndex<0 || accountIndex>1) return -1;
        active=accountIndex;
        startedNanos=System.nanoTime();
        return active;
    }
    synchronized void stop() {
        if (active >= 0) {
            lastElapsedSeconds=elapsedSeconds();
            lastActive=active;
        }
        active=-1;
        startedNanos=0;
    }
    synchronized int activeIndex() {return active;}
    synchronized boolean activeA() {return active==0;}
    private long elapsedSeconds() {
        return active < 0 || startedNanos == 0 ? 0 :
            Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000_000L);
    }
    private int events(int cohort) {
        int total=0;
        for (Count count : counts[cohort]) total+=count.observed + count.dropped;
        return total;
    }
    private static String clock(long elapsedSeconds) {
        return String.format(Locale.ROOT, "%02d:%02d", elapsedSeconds/60, elapsedSeconds%60);
    }
    /** Lightweight, privacy-safe UI snapshot; no polling of Instagram structures. */
    synchronized String feedback() {
        if (active >= 0) {
            int total=events(active);
            return "● RECORDING " + (active==0 ? "A" : "B") + " · " +
                    clock(elapsedSeconds()) + " · " + total + " gate events" +
                    (total==0 ? " (browse a post/Reel)" : "");
        }
        if (lastActive >= 0) {
            return "■ STOPPED · last " + (lastActive==0 ? "A" : "B") + " · " +
                    clock(lastElapsedSeconds) + " · " + events(lastActive) + " events";
        }
        return "● READY · press Start A or Start B";
    }
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
            if (SELECTORS[i]==selector) {counts[active][i].record(selector,result);return;}
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
                 .append(" type=").append(c.classCounts.size()>1?"MIXED":c.lastObjectClass)
                 .append("\n")
                 .append("  Per-result code classes: ").append(c.classCounts)
                 .append(" unlisted=").append(c.unlistedClasses).append("\n");
                if (i==0 || i==1) {
                    b.append("  IG455 object-shape: singleton=").append(c.singletonVariant)
                     .append(" payload=").append(c.payloadVariant)
                     .append(" unknown=").append(c.unknownVariant)
                     .append(" shape_mismatch=").append(c.invalidShape).append("\n")
                     .append("  payload integer A00: zero=").append(c.intZero)
                     .append(" positive=").append(c.intPositive)
                     .append(" negative=").append(c.intNegative).append("\n");
                    if(i==0) b.append("  payload booleans: A06(true/false)=")
                        .append(c.flag06True).append('/').append(c.flag06False)
                        .append(" A07(true/false)=")
                        .append(c.flag07True).append('/').append(c.flag07False)
                        .append("\n  non-null fields across payload samples: references=")
                        .append(c.referenceFieldsPresent).append(" strings=")
                        .append(c.stringFieldsPresent).append("\n");
                }
            }
            b.append("Visible UI label counts: ").append(uiSamples[a]).append("\n");
        }
        b.append("Interpretation: selection-specific method outcomes are NOT proof of account eligibility.\n")
         .append("IG455 object shape fields are structural metadata; A00/A06/A07 semantics NOT confirmed.\n")
         .append("Only primitive buckets and reference presence totals recorded; values and objects never exported.\n")
         .append("Non-Boolean result objects are NOT coerced, mutated, or printed.\n")
         .append("Captures are volatile and disappear when the Instagram process exits.\n");
        return b.toString();
    }
}
