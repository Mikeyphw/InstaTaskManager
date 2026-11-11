package com.mikeyphw.instataskmanager;

/**
 * Exact IG455 Repost selector-4, read-only upstream predicate telemetry.
 * No account/media identifier, configuration key, method argument or reference
 * value is retained. Each frame lives only within the ORIGINAL invoke() call.
 * Instrumented getters are otherwise entirely passive.
 */
final class P04PredicateTrace {
    static final int MOBILE_CONFIG = 0;     // X.02ja.BdA(J):Z concrete implementation
    static final int ELIGIBILITY = 1;       // X.02ib.D3r(I):Boolean concrete implementation (nullable)
    static final int SESSION_CHECK = 2;     // X.02xy.A04(UserSession,String):Z
    private static final int SOURCES = 3;
    private static final int MAX_ORDINAL = 4;
    private static final int OUTCOMES = 4;  // TRUE, FALSE, NULL, OTHER
    private static final int MAX_FRAMES = 256;
    private static final String[] NAMES = {
        "MobileConfig X.02ja Boolean (call ordinal)",
        "Config X.02ib nullable Boolean (call ordinal)",
        "Repost session check Boolean (call ordinal)"
    };
    // Counters for A/B, outer state singleton/payload/other, getter, ordinal, outcome
    private static final int[][][][][] COUNTS = new int[2][3][SOURCES][MAX_ORDINAL][OUTCOMES];
    private static final int[][] FRAME_COUNTS = new int[2][3];
    // A/B cohort, observed return state, and early-branch input presence only.
    private static final int[][][] INPUT_COUNTS = new int[2][3][4];
    private static final int[][] DROPPED_FRAMES = new int[2][3];
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();
    private P04PredicateTrace() { }

    static final class Frame {
        final int cohort;
        final int inputPresence;
        final Frame parent;
        final int[][] values = new int[SOURCES][MAX_ORDINAL];
        final int[] next = new int[SOURCES];
        Frame(int cohort, Frame parent, int inputPresence) {
            this.cohort=cohort;
            this.parent=parent;
            this.inputPresence=inputPresence;
            for(int i=0;i<SOURCES;i++)java.util.Arrays.fill(values[i],-1);
        }
    }

    static Frame enter(int cohort) {return enter(cohort,P04InputPresence.UNAVAILABLE);}
    static Frame enter(int cohort,int presence) {
        if(cohort!=0 && cohort!=1)return null;
        if(presence<0 || presence>=4)presence=P04InputPresence.UNAVAILABLE;
        Frame frame=new Frame(cohort,ACTIVE.get(),presence);
        ACTIVE.set(frame);
        return frame;
    }

    // Only called inside the Xposed interceptors for exactly verified getter signatures.
    static void note(int source, Object value) {
        Frame frame=ACTIVE.get();
        if(frame==null || source<0 || source>=SOURCES)return;
        int ordinal=frame.next[source]++;
        if(ordinal>=MAX_ORDINAL)return; // no allocations or raw value retention
        frame.values[source][ordinal]=Boolean.TRUE.equals(value)?0:
                Boolean.FALSE.equals(value)?1:value==null?2:3;
    }

    /** In a finally block, ALWAYS restore the caller's ThreadLocal context. */
    static void leave(Frame frame, Object originalResult) {
        if(frame==null)return;
        if(ACTIVE.get()!=frame){ACTIVE.remove();return;} // fail closed on scope mismatch
        ACTIVE.set(frame.parent);
        int state=classify(originalResult);
        synchronized(P04PredicateTrace.class){
            if(FRAME_COUNTS[frame.cohort][state]>=MAX_FRAMES){DROPPED_FRAMES[frame.cohort][state]++;return;}
            FRAME_COUNTS[frame.cohort][state]++;
            INPUT_COUNTS[frame.cohort][state][frame.inputPresence]++;
            for(int source=0;source<SOURCES;source++)
                for(int ordinal=0;ordinal<MAX_ORDINAL;ordinal++){
                    int value=frame.values[source][ordinal];
                    if(value>=0)COUNTS[frame.cohort][state][source][ordinal][value]++;
                }
        }
    }

    static int classify(Object originalResult){
        if(originalResult==null)return 2;
        String name=originalResult.getClass().getName();
        if("X.09j4".equals(name))return 0;
        if("X.09ZC".equals(name))return 1;
        return 2;
    }

    static synchronized void clear(){
        for(int a=0;a<2;a++)for(int s=0;s<3;s++){
            FRAME_COUNTS[a][s]=0;
            DROPPED_FRAMES[a][s]=0;
            java.util.Arrays.fill(INPUT_COUNTS[a][s],0);
            for(int gate=0;gate<SOURCES;gate++)
                for(int ordinal=0;ordinal<MAX_ORDINAL;ordinal++)
                    java.util.Arrays.fill(COUNTS[a][s][gate][ordinal],0);
        }
    }

    static synchronized String report(){
        StringBuilder out=new StringBuilder("P04 HF03 UPSTREAM PREDICATES (read-only, selector 4 only)\n");
        out.append("Concrete implementation coverage is PARTIAL; no values means NOT OBSERVED, not false.\nCall ordinals are within one original invoke(); no configuration arguments, keys or values exported.\n");
        for(int account=0;account<2;account++){
            out.append("Manual account ").append(account==0?'A':'B').append(':').append('\n');
            for(int variant=0;variant<3;variant++){
                int frames=FRAME_COUNTS[account][variant];
                int dropped=DROPPED_FRAMES[account][variant];
                if(frames==0 && dropped==0)continue;
                out.append("  outer state ").append(variant==0?"singleton":variant==1?"payload":"other")
                   .append(" frames=").append(frames).append(" capped_extra=").append(dropped).append('\n');
                out.append("   HF04 early-branch input presence: ");
                for(int k=0;k<4;k++){
                    if(k>0)out.append(" ");
                    out.append(P04InputPresence.LABELS[k]).append("=").append(INPUT_COUNTS[account][variant][k]);
                }
                out.append('\n');
                for(int source=0;source<SOURCES;source++)
                    for(int ordinal=0;ordinal<MAX_ORDINAL;ordinal++){
                        int[] values=COUNTS[account][variant][source][ordinal];
                        int n=values[0]+values[1]+values[2]+values[3];
                        if(n==0)continue;
                        out.append("   ").append(NAMES[source]).append(" #").append(ordinal+1)
                           .append(": true=").append(values[0])
                           .append(" false=").append(values[1])
                           .append(" null=").append(values[2])
                           .append(" other=").append(values[3]).append('\n');
                    }
            }
        }
        out.append("Values remain unmodified. Different ordinals may reflect different branch paths, not identical checks.\n");
        return out.toString();
    }
}
