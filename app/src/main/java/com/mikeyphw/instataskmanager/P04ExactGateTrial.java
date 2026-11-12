package com.mikeyphw.instataskmanager;

/**
 * Session-only experimental Repost UI eligibility trial, never a repost action.
 * Native IG455 selector 4 has two exact predicate calls. We change at most six
 * Boolean.FALSE results to TRUE, only after repeated A/B evidence for the SAME
 * exact call with the Repost data object present. No server request or identity
 * is modified by this code. This can fail to reveal a button and may require
 * restarting Instagram to restore native rendering.
 */
final class P04ExactGateTrial {
    static final int FIRST_CONFIG=0, DATA_ELIGIBILITY=1;
    static final int GATES=2, OUTCOMES=4; // true, false, null, other
    static final int LIMIT=6;
    static final long DURATION_NS=30_000_000_000L;
    static final String[] LABELS={"FIRST_MOBILE_CONFIG", "REPOST_DATA_BOOLEAN"};
    // Cohort, gate, original outcome, terminal state (singleton/payload/other)
    private static final int[][][][] counts=new int[2][GATES][OUTCOMES][3];
    private static final int[][] presentCounts=new int[2][3]; // missing/present/unavailable
    private static boolean deoptimized;
    private static int armedGate=-1, changed;
    private static long deadline;
    private P04ExactGateTrial() { }

    static synchronized void setDeoptimized(boolean ok) { deoptimized=ok; if(!ok)disarm(); }
    static synchronized boolean isDeoptimized() {return deoptimized;}
    static synchronized void reset() {
        disarm();changed=0;
        for(int cohort=0;cohort<2;cohort++){
            java.util.Arrays.fill(presentCounts[cohort],0);
            for(int gate=0;gate<GATES;gate++)
                for(int outcome=0;outcome<OUTCOMES;outcome++)
                    java.util.Arrays.fill(counts[cohort][gate][outcome],0);
        }
    }
    static synchronized void disarm() {armedGate=-1;deadline=0;}
    static int outcome(Object value) {
        return Boolean.TRUE.equals(value)?0:Boolean.FALSE.equals(value)?1:
            value==null?2:3;
    }
    static synchronized void recordFrame(int cohort,int dataPresence,int[] originals,int terminal) {
        if(cohort<0 || cohort>1 || terminal<0 || terminal>=3)return;
        if(dataPresence>=0 && dataPresence<=2)presentCounts[cohort][dataPresence]++;
        if(dataPresence!=P04BranchInputs.PRESENT)return;
        for(int gate=0;gate<GATES;gate++){
            int v=originals[gate];
            if(v>=0 && v<OUTCOMES)counts[cohort][gate][v][terminal]++;
        }
    }
    static synchronized int eligibleGate() {
        if(!deoptimized) return -1;
        // No missing data object in either cohort may be converted to a fake payload.
        if(presentCounts[0][P04BranchInputs.MISSING]!=0 ||
           presentCounts[1][P04BranchInputs.MISSING]!=0 ||
           presentCounts[0][P04BranchInputs.UNAVAILABLE]!=0 ||
           presentCounts[1][P04BranchInputs.UNAVAILABLE]!=0)return -1;
        for(int gate=0;gate<GATES;gate++){
            int aF=counts[0][gate][1][0]; // A false -> native singleton
            int bT=counts[1][gate][0][1]; // B true -> native payload
            if(aF<3 || bT<3)continue;
            int rest=0;
            for(int a=0;a<2;a++)for(int o=0;o<OUTCOMES;o++)for(int st=0;st<3;st++){
                if((a==0 && o==1 && st==0) || (a==1 && o==0 && st==1))continue;
                rest+=counts[a][gate][o][st];
            }
            if(rest==0) return gate;
        }
        return -1;
    }
    static synchronized String reason() {
        if(!deoptimized)return "Caller deoptimization unavailable; gate trial disabled";
        if(presentCounts[0][P04BranchInputs.MISSING]>0 ||
           presentCounts[1][P04BranchInputs.MISSING]>0)
            return "Native A03 Repost data object missing; no safe Boolean-only fix";
        if(presentCounts[0][P04BranchInputs.UNAVAILABLE]>0 ||
           presentCounts[1][P04BranchInputs.UNAVAILABLE]>0)
            return "Native Repost data shape unavailable; refusing override";
        if(presentCounts[0][P04BranchInputs.PRESENT]<3 ||
           presentCounts[1][P04BranchInputs.PRESENT]<3)
            return "Capture at least 3 Repost-state calls on each account";
        int observedA=0,observedB=0;
        for(int g=0;g<GATES;g++)for(int o=0;o<OUTCOMES;o++)for(int st=0;st<3;st++){
            observedA+=counts[0][g][o][st];observedB+=counts[1][g][o][st];
        }
        if(observedA==0 || observedB==0)
            return "No exact eligibility getter callbacks; factory dispatch may be different";
        int candidate=eligibleGate();
        if(candidate>=0)return "Qualified "+LABELS[candidate]+"; Start A and press Try A UI gate";
        return "A/B exact gate outcomes do not have a clean false→true split; no override";
    }
    static synchronized String arm(int activeCohort) {
        disarm();
        if(activeCohort!=0)return "REFUSED: Start A on the affected account first.";
        if(!deoptimized)return "REFUSED: IG455 caller deoptimization unavailable; original methods only.";
        int g=eligibleGate();
        if(g<0)return "REFUSED: "+reason()+". No change made.";
        armedGate=g;changed=0;deadline=System.nanoTime()+DURATION_NS;
        return "ARMED: "+LABELS[g]+" · at most "+LIMIT+" false→true UI checks for 30 s, Account A only.";
    }
    /** Called only inside the original IG455 selector 4 stack and exact gate. */
    static synchronized Object maybeOverride(int cohort,int dataPresence,int gate,Object original) {
        if(armedGate<0 || System.nanoTime()>deadline || changed>=LIMIT){disarm();return original;}
        if(!deoptimized || cohort!=0 || dataPresence!=P04BranchInputs.PRESENT ||
                gate!=armedGate || !Boolean.FALSE.equals(original))return original;
        changed++;
        if(changed>=LIMIT)disarm();
        return Boolean.TRUE;
    }
    static synchronized String status() {
        if(armedGate>=0 && System.nanoTime()>deadline)disarm();
        if(armedGate>=0)return "ARMED "+LABELS[armedGate]+" · altered "+changed+"/"+LIMIT;
        return "OFF · altered "+changed+"/"+LIMIT;
    }
    static synchronized String report() {
        StringBuilder out=new StringBuilder("P04 HF08 EXACT REPOST BRANCH (original results only)\n");
        out.append("Deoptimized caller: ").append(deoptimized?"YES":"NO")
           .append(" | UI trial ").append(status()).append("\n");
        for(int a=0;a<2;a++){
            out.append("Manual ").append(a==0?'A':'B').append(" A03 Repost data: missing=")
               .append(presentCounts[a][0]).append(" present=")
               .append(presentCounts[a][1]).append(" unavailable=")
               .append(presentCounts[a][2]).append('\n');
            for(int g=0;g<GATES;g++){
                out.append(" ").append(LABELS[g]).append(" original (singleton/payload/other): ");
                for(int o=0;o<OUTCOMES;o++){
                    if(o>0)out.append(' ');
                    out.append(o==0?"true":o==1?"false":o==2?"null":"other").append('=');
                    for(int s=0;s<3;s++){
                        if(s>0)out.append('/');
                        out.append(counts[a][g][o][s]);
                    }
                }
                out.append('\n');
            }
        }
        out.append("Eligible guarded trial: ")
           .append(eligibleGate()<0?"NO":LABELS[eligibleGate()])
           .append(" | Reason: ").append(reason())
           .append(". Only exact IG455 predicates; no account identity or configuration values retained.\n");
        return out.toString();
    }
}
