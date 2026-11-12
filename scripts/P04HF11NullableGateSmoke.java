package com.mikeyphw.instataskmanager;

/** HF11 synthetic original-only eligibility trials and adversarial cases. */
public final class P04HF11NullableGateSmoke {
    private static int n;
    private static void ok(boolean yes,String what){if(!yes)throw new AssertionError(what);n++;}
    private static void sample(int a,int gate,int outcome,int terminal){
        int[] orig={-1,-1};orig[gate]=outcome;
        P04ExactGateTrial.recordFrame(a,P04BranchInputs.PRESENT,orig,terminal);
    }
    private static void baseline(int nulls){
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<42;i++)sample(0,1,1,0);
        for(int i=0;i<nulls;i++)sample(0,1,2,0);
        for(int i=0;i<31;i++)sample(1,1,0,1);
        // 50th A frame contained no observed D3r result; preserve absence.
        P04ExactGateTrial.recordFrame(0,P04BranchInputs.PRESENT,new int[]{-1,-1},0);
    }
    public static void main(String[] args){
        baseline(7);
        ok(P04ExactGateTrial.eligibleGate()==1,"captured 42 false + 7 null, B 31 true qualifies");
        ok(P04ExactGateTrial.reason().contains("null results will be preserved"),"instruct preserve null");
        ok(P04ExactGateTrial.arm(1).startsWith("REFUSED"),"never arm on B");
        ok(P04ExactGateTrial.arm(0).startsWith("ARMED"),"manual A arm");
        ok(P04ExactGateTrial.maybeOverride(0,1,1,null)==null,"null preserved");
        ok(Boolean.TRUE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.TRUE)),"true preserved");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(1,1,1,Boolean.FALSE)),"B preserved");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,0,Boolean.FALSE)),"wrong gate preserved");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,0,1,Boolean.FALSE)),"missing data preserved");
        for(int i=0;i<6;i++)ok(Boolean.TRUE.equals(
            P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"bounded false override "+i);
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"cap reverts to original");
        ok(P04ExactGateTrial.status().startsWith("OFF"),"auto-disarm");
        // Null-heavy A cannot qualify, even if there are some false observations.
        baseline(20);ok(P04ExactGateTrial.eligibleGate()<0,"A null share above 25% refuses");
        baseline(7);sample(0,1,0,0);ok(P04ExactGateTrial.eligibleGate()<0,"unexpected A true refuses");
        baseline(7);sample(0,1,3,0);ok(P04ExactGateTrial.eligibleGate()<0,"unexpected A other refuses");
        baseline(7);sample(1,1,1,1);ok(P04ExactGateTrial.eligibleGate()<0,"B false refuses");
        baseline(7);sample(1,1,2,1);ok(P04ExactGateTrial.eligibleGate()<0,"B null refuses");
        baseline(7);sample(0,1,1,1);ok(P04ExactGateTrial.eligibleGate()<0,"A false payload refuses");
        baseline(7);sample(0,1,2,1);ok(P04ExactGateTrial.eligibleGate()<0,"A null payload refuses");
        baseline(7);sample(1,1,0,0);ok(P04ExactGateTrial.eligibleGate()<0,"B true singleton refuses");
        baseline(7);P04ExactGateTrial.recordFrame(0,P04BranchInputs.MISSING,
            new int[]{-1,1},0);ok(P04ExactGateTrial.eligibleGate()<0,"missing A03 refuses");
        baseline(7);P04ExactGateTrial.recordFrame(1,P04BranchInputs.UNAVAILABLE,
            new int[]{-1,0},1);ok(P04ExactGateTrial.eligibleGate()<0,"unavailable data refuses");
        baseline(7);P04ExactGateTrial.setDeoptimized(false);
        ok(P04ExactGateTrial.eligibleGate()<0,"deopt failure refuses");
        baseline(7);ok(P04ExactGateTrial.arm(0).startsWith("ARMED"),"arm for contradiction test");
        sample(0,1,0,0);
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"requalification disarms");
        ok(P04ExactGateTrial.status().startsWith("OFF"),"contradiction OFF");
        // A successful UI trial may produce a PAYLOAD terminal state even
        // though the D3r original was false; this is NOT a contradictory
        // original A/B sample and must not poison qualification.
        P04PredicateTrace.clear(); baseline(7);
        ok(P04ExactGateTrial.arm(0).startsWith("ARMED"),"arm frame contamination test");
        for(int i=0;i<6;i++){
            P04PredicateTrace.Frame trialFrame=P04PredicateTrace.enter(0,
                P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
            Object patched=P04PredicateTrace.noteExactData(
                Integer.valueOf(0xdf82522e),Boolean.FALSE);
            P04PredicateTrace.leave(trialFrame,new Object());
            ok(Boolean.TRUE.equals(patched),"intervention remains qualified #"+i);
        }
        ok(P04ExactGateTrial.eligibleGate()==P04ExactGateTrial.DATA_ELIGIBILITY,
            "intervention frames do not contaminate baseline A/B");
        ok(P04ExactGateTrial.status().startsWith("OFF"),"intervention cap ends test");
        baseline(7);P04ExactGateTrial.disarm();
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"manual OFF preserves result");
        ok(P04ExactGateTrial.report().contains("null stays native"),"explanation exported");
        System.out.println("ITM_P04_HF11_NULL_SAFE_EXACT_GATE_AND_NEGATIVES_PASS="+n);
    }
}
