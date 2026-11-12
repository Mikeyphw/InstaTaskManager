package com.mikeyphw.instataskmanager;

/** HF12 source-bound 2026-11-12 actual capture: A false42/unobserved4; B true26/false singleton1. */
public final class P04HF12OutlierSmoke {
    private static int n;
    private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);n++;}
    private static void add(int cohort,int outcome,int state){
        int[] v={-1,outcome};
        P04ExactGateTrial.recordFrame(cohort,P04BranchInputs.PRESENT,v,state);
    }
    private static void baseline(){
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<42;i++)add(0,1,0);
        for(int i=0;i<4;i++)P04ExactGateTrial.recordFrame(0,P04BranchInputs.PRESENT,new int[]{-1,-1},0);
        for(int i=0;i<26;i++)add(1,0,1);
        add(1,1,0); // exact minority B singleton from user's report
    }
    public static void main(String[] args){
        baseline();
        check(P04ExactGateTrial.eligibleGate()==P04ExactGateTrial.DATA_ELIGIBILITY,"actual A/B evidence qualifies bounded trial");
        check(P04ExactGateTrial.reason().contains("HF12 EXPERIMENTAL"),"experiment clearly labeled");
        check(P04ExactGateTrial.arm(1).startsWith("REFUSED"),"B never arms");
        check(P04ExactGateTrial.arm(0).startsWith("ARMED: HF12 EXPERIMENTAL"),"A arms explicitly");
        check(P04ExactGateTrial.maybeOverride(0,1,1,null)==null,"null never coerced");
        check(Boolean.TRUE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.TRUE)),"true preserved");
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(1,1,1,Boolean.FALSE)),"B untouched");
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,0,1,Boolean.FALSE)),"missing native data not overridden");
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,0,Boolean.FALSE)),"first config not targeted");
        for(int i=0;i<6;i++)
            check(Boolean.TRUE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"bounded exact A false #"+i);
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"6-change cap");
        check(P04ExactGateTrial.status().startsWith("OFF"),"auto disarm");
        baseline();add(1,1,0);
        check(P04ExactGateTrial.eligibleGate()<0,"two B false singletons disqualify");
        baseline();add(1,1,1);
        check(P04ExactGateTrial.eligibleGate()<0,"B false payload disqualifies");
        baseline();add(1,2,0);
        check(P04ExactGateTrial.eligibleGate()<0,"B null disqualifies");
        baseline();add(0,0,0);
        check(P04ExactGateTrial.eligibleGate()<0,"A true contradictory");
        baseline();add(0,1,1);
        check(P04ExactGateTrial.eligibleGate()<0,"A false payload contradictory");
        baseline();P04ExactGateTrial.recordFrame(0,P04BranchInputs.MISSING,new int[]{-1,1},0);
        check(P04ExactGateTrial.eligibleGate()<0,"native A data missing");
        baseline();P04ExactGateTrial.recordFrame(1,P04BranchInputs.UNAVAILABLE,new int[]{-1,0},1);
        check(P04ExactGateTrial.eligibleGate()<0,"native B data unknown");
        baseline();P04ExactGateTrial.setDeoptimized(false);
        check(P04ExactGateTrial.eligibleGate()<0,"no deopt refuses");
        baseline();P04ExactGateTrial.arm(0);add(1,1,0);
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"new B contradiction immediately revokes");
        check(P04ExactGateTrial.status().startsWith("OFF"),"revoked trial OFF");
        // Emulate six live interventions via actual exact-key trace callback.
        P04PredicateTrace.clear();baseline();
        P04ExactGateTrial.arm(0);
        for(int i=0;i<6;i++){
            P04PredicateTrace.Frame f=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
            Object out=P04PredicateTrace.noteExactData(Integer.valueOf(0xdf82522e),Boolean.FALSE);
            P04PredicateTrace.leave(f,new Object());
            check(Boolean.TRUE.equals(out),"exact original callback patched #"+i);
        }
        check(P04ExactGateTrial.eligibleGate()==P04ExactGateTrial.DATA_ELIGIBILITY,"interventions not added to baseline");
        check(P04ExactGateTrial.status().startsWith("OFF"),"sixth change expires trial");
        P04PredicateTrace.clear();baseline();P04ExactGateTrial.arm(0);
        P04PredicateTrace.Frame f=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
        Object wrong=P04PredicateTrace.noteExactData(Integer.valueOf(0x12345678),Boolean.FALSE);
        P04PredicateTrace.leave(f,new Object());
        check(Boolean.FALSE.equals(wrong),"different config key untouched");
        P04ExactGateTrial.disarm();
        check(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"Trial OFF immediate");
        System.out.println("ITM_P04_HF12_ACTUAL_AB_RELAXED_OUTLIER_AND_NEGATIVE_PASS="+n);
    }
}
