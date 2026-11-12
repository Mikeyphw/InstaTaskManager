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
    /**
     * HF11: the exact IG455 D3r data gate is nullable. A null result is not
     * FALSE, is never changed, and may coexist with a large, repeated false
     * cohort. Qualify only the false branch, with explicit null and coverage
     * bounds, a strongly-positive B cohort (one <=5% false singleton outlier),
     * native data, and the original return shape. This is a user-initiated,
     * EXPERIMENTAL UI-only branch trial; the server still decides permissions.
     * FIRST_CONFIG retains the original clean split requirement.
     */
    static synchronized int eligibleGate() {
        if(!deoptimized) return -1;
        if(presentCounts[0][P04BranchInputs.MISSING]!=0 ||
           presentCounts[1][P04BranchInputs.MISSING]!=0 ||
           presentCounts[0][P04BranchInputs.UNAVAILABLE]!=0 ||
           presentCounts[1][P04BranchInputs.UNAVAILABLE]!=0)return -1;
        for(int gate=0;gate<GATES;gate++){
            int aF=counts[0][gate][1][0]; // A false -> native singleton
            int bT=counts[1][gate][0][1]; // B true -> native payload
            if(aF<5 || bT<5)continue;
            int aNull=gate==DATA_ELIGIBILITY?counts[0][gate][2][0]:0;
            if(gate==DATA_ELIGIBILITY){
                // A must be predominantly false, not mostly null (>=75%).
                if((long)aF*4L < (long)(aF+aNull)*3L)continue;
                // No large unobserved population: >=75% of native-data frames
                // must contain this exact-key result, on BOTH accounts.
                if((long)(aF+aNull)*4L <
                        (long)presentCounts[0][P04BranchInputs.PRESENT]*3L ||
                   (long)bT*4L <
                        (long)presentCounts[1][P04BranchInputs.PRESENT]*3L)continue;
            }
            // HF12: the observed working cohort had 26 TRUE/payload and one
            // FALSE/singleton. Permit that single content-specific outlier
            // ONLY for D3r, with at least 10 working TRUE samples and <=5%
            // original FALSE singleton observations. Never permit B false
            // payload, null, non-Boolean, or a second contradictory result.
            int bFalseSingleton=counts[1][DATA_ELIGIBILITY][1][0];
            if(gate==DATA_ELIGIBILITY){
                if(bFalseSingleton>1 || (bFalseSingleton>0 && (aF<10 || bT<10)) ||
                   (long)bFalseSingleton*20L>(long)(bT+bFalseSingleton))continue;
            }
            int mismatched=0;
            for(int a=0;a<2;a++)for(int o=0;o<OUTCOMES;o++)for(int st=0;st<3;st++){
                if((a==0 && o==1 && st==0) || (a==1 && o==0 && st==1))continue;
                // Null in A is only permissible on the exact nullable D3r
                // gate, and ONLY with the same singleton terminal state.
                if(gate==DATA_ELIGIBILITY && a==0 && o==2 && st==0)continue;
                // HF12 limited manual trial: tolerate only ONE B false that
                // already resulted in a native singleton (never a payload).
                if(gate==DATA_ELIGIBILITY && a==1 && o==1 && st==0 &&
                   bFalseSingleton<=1)continue;
                mismatched+=counts[a][gate][o][st];
            }
            if(mismatched==0)return gate;
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
        if(candidate>=0){
            if(candidate==DATA_ELIGIBILITY)
                return "HF12 EXPERIMENTAL: A false-only trial eligible; one B singleton false outlier permitted (<=5%); A null results will be preserved; Start A then Try A UI gate";
            return "Qualified "+LABELS[candidate]+"; Start A and press Try A UI gate";
        }
        return "A/B exact gate unqualified: need >=10 A false singleton, >=10 B true payload, at most one B false singleton (<=5%), intact native data, no other contradictions; no override";
    }
    static synchronized String arm(int activeCohort) {
        disarm();
        if(activeCohort!=0)return "REFUSED: Start A on the affected account first.";
        if(!deoptimized)return "REFUSED: IG455 caller deoptimization unavailable; original methods only.";
        int g=eligibleGate();
        if(g<0)return "REFUSED: "+reason()+". No change made.";
        armedGate=g;changed=0;deadline=System.nanoTime()+DURATION_NS;
        return "ARMED: HF12 EXPERIMENTAL "+LABELS[g]+" · "+LIMIT+" false→true local UI checks / 30 s max, Account A only; no automatic repost.";
    }
    /** Called only inside the original IG455 selector 4 stack and exact gate. */
    static synchronized Object maybeOverride(int cohort,int dataPresence,int gate,Object original) {
        if(armedGate<0 || System.nanoTime()>deadline || changed>=LIMIT){disarm();return original;}
        if(!deoptimized || cohort!=0 || dataPresence!=P04BranchInputs.PRESENT ||
                gate!=armedGate || !Boolean.FALSE.equals(original))return original;
        // A new contradiction or changed capture population immediately
        // revokes qualification; never keep using stale A/B evidence.
        if(eligibleGate()!=armedGate){disarm();return original;}
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
        out.append("HF12 EXPERIMENTAL: exact D3r A false-only override may tolerate one B false/singleton outlier <=5%; A/B native data required; null stays native and is never modified.\n");
        out.append("Eligible guarded trial: ")
           .append(eligibleGate()<0?"NO":LABELS[eligibleGate()])
           .append(" | Reason: ").append(reason())
           .append(". Only exact IG455 predicates; no account identity or configuration values retained.\n");
        return out.toString();
    }
}
