package com.mikeyphw.instataskmanager;

/** Pure-Java offline trials, never a live Instagram permission demonstration. */
public final class P04HF08GateSmoke {
    static int passes;
    static void ok(boolean v,String s) {if(!v)throw new AssertionError(s);passes++;}
    static void sample(int cohort,int gate,int outcome,int variant) {
        int[] x={-1,-1};x[gate]=outcome;
        P04ExactGateTrial.recordFrame(cohort,P04BranchInputs.PRESENT,x,variant);
    }
    public static void main(String[] args) {
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(false);
        ok(P04ExactGateTrial.eligibleGate()<0,"no trial with deopt off");
        ok(P04ExactGateTrial.arm(0).startsWith("REFUSED"),"not armed before observing");
        P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<6;i++){
            sample(0,0,1,0); // A false -> singleton
            sample(1,0,0,1); // B true -> payload
        }
        ok(P04ExactGateTrial.eligibleGate()==P04ExactGateTrial.FIRST_CONFIG,"exact split discovered");
        ok(P04ExactGateTrial.arm(1).startsWith("REFUSED"),"cannot arm B");
        ok(P04ExactGateTrial.arm(0).startsWith("ARMED"),"only A can arm");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(1,1,0,Boolean.FALSE)),"B never modified");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,0,0,Boolean.FALSE)),"missing data not modified");
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,1,Boolean.FALSE)),"wrong gate not modified");
        ok(Boolean.TRUE.equals(P04ExactGateTrial.maybeOverride(0,1,0,Boolean.TRUE)),"native true stays true");
        for(int i=0;i<6;i++) ok(Boolean.TRUE.equals(P04ExactGateTrial.maybeOverride(0,1,0,Boolean.FALSE)),"bounded override "+i);
        ok(Boolean.FALSE.equals(P04ExactGateTrial.maybeOverride(0,1,0,Boolean.FALSE)),"auto-off cap");
        ok(P04ExactGateTrial.status().startsWith("OFF"),"trial auto-disarmed");
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<6;i++){sample(0,0,1,0);sample(1,0,0,1);}
        sample(0,0,0,0); // mixed A outcomes
        ok(P04ExactGateTrial.eligibleGate()<0,"mixed refuses");
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<6;i++){sample(0,0,1,0);sample(1,0,0,1);}
        P04ExactGateTrial.recordFrame(0,P04BranchInputs.MISSING,new int[]{1,-1},0);
        ok(P04ExactGateTrial.eligibleGate()<0,"missing input refuses");
        P04ExactGateTrial.reset();P04ExactGateTrial.setDeoptimized(true);
        for(int i=0;i<6;i++){sample(0,1,1,0);sample(1,1,0,1);}
        ok(P04ExactGateTrial.eligibleGate()==P04ExactGateTrial.DATA_ELIGIBILITY,"alternate exact gate allowed only after evidence");
        ok(!P04ExactGateTrial.report().contains("AccountId"),"no identifiers in report");
        ok(P04ExactGateTrial.report().contains("A03 Repost data"),"A03 stats visible");
        P04ExactGateTrial.disarm();
        System.out.println("ITM_P04_HF08_EXACT_GATE_AND_RESTORATION_PASS="+passes);
    }
}
