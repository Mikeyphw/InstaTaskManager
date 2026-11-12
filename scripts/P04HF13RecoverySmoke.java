package com.mikeyphw.instataskmanager;

/** HF13 evidence, lifecycle, races, strict original-keys and rollback suite. */
public final class P04HF13RecoverySmoke {
    private static int checks;
    private static long now=100_000_000_000L;
    private static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);checks++;}
    private static void frame(int a,int code,int term){
        P04ExactGateTrial.recordFrame(a,P04BranchInputs.PRESENT,new int[]{-1,code},term);
    }
    private static void setup() {
        P04HF13Recovery.off("new baseline");
        P04ExactGateTrial.reset();
        P04ExactGateTrial.setDeoptimized(true);
        P04ExactGateTrial.onCaptureStart(0);
        for(int i=0;i<12;i++)frame(0,1,0);
        ok(P04ExactGateTrial.eligibleForHF13Recovery(),"fresh A-only evidence from proven HF12");
    }
    private static boolean falseValue(Object x) {return Boolean.FALSE.equals(x);}
    private static boolean trueValue(Object x) {return Boolean.TRUE.equals(x);}
    private static Object exact(int cohort,Object key,Object original) {
        P04PredicateTrace.Frame f=P04PredicateTrace.enter(cohort,P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
        Object changed=P04PredicateTrace.noteExactData(key,original);
        // Synthetic Object is not a real IG455 UI variant; do not allow it to
        // contaminate the exact-native eligibility baseline under test.
        f.changedByTrial=true;
        P04PredicateTrace.leave(f,new Object());
        return changed;
    }
    public static void main(String[] args) throws Exception {
        P04HF13Recovery.setClockForTest(()->now);
        setup();
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"default OFF");
        ok(P04HF13Recovery.arm(1,true,true).startsWith("REFUSED"),"B cannot arm");
        ok(P04HF13Recovery.arm(0,false,true).startsWith("REFUSED"),"unverified exact APK");
        ok(P04HF13Recovery.arm(0,true,false).startsWith("REFUSED"),"missing native receiver");
        ok(P04HF13Recovery.arm(0,true,true).startsWith("ENABLED"),"manual Reels-tab consent");
        ok(exact(0,0xdf82522e,null)==null,"nullable getter remains nullable");
        ok(trueValue(exact(0,0xdf82522e,Boolean.TRUE)),"native true unchanged");
        ok(falseValue(exact(0,0xdf82522f,Boolean.FALSE)),"wrong int key unchanged");
        ok(falseValue(P04HF13Recovery.maybeOverride(P04HF13Recovery.epoch(),0,
            P04BranchInputs.MISSING,false,Boolean.FALSE)),"native A03 missing fail closed");
        ok(falseValue(P04HF13Recovery.maybeOverride(P04HF13Recovery.epoch(),0,
            P04BranchInputs.UNAVAILABLE,false,Boolean.FALSE)),"unavailable data fail closed");
        ok(falseValue(exact(1,0xdf82522e,Boolean.FALSE)),"B never changed");
        ok(trueValue(exact(0,0xdf82522e,Boolean.FALSE)),"HF12 native Repost gate works");
        // Nested / cross-generation work must not leak into any new capture.
        P04PredicateTrace.Frame parent=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
        P04PredicateTrace.Frame nested=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT,P04BranchInputs.PRESENT);
        ok(falseValue(P04PredicateTrace.noteExactData(0xdf82522e,Boolean.FALSE)),"nested frame refused");
        nested.changedByTrial=true;
        parent.changedByTrial=true;
        P04PredicateTrace.leave(nested,null);
        P04PredicateTrace.leave(parent,null);
        long prior=P04HF13Recovery.epoch();
        P04HF13Recovery.onCaptureTransition();
        ok(falseValue(P04HF13Recovery.maybeOverride(prior,0,P04BranchInputs.PRESENT,false,Boolean.FALSE)),
            "capture/account switch disarms in-flight frame");
        ok(!P04HF13Recovery.isEnabled(),"switch OFF");
        setup();P04HF13Recovery.arm(0,true,true);
        P04HF13Recovery.onActivityExit();
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"pause/destroy immediately OFF");
        setup();P04HF13Recovery.arm(0,true,true);
        now+=P04HF13Recovery.LEASE_NS;
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"lease timeout fail closed");
        setup();P04HF13Recovery.arm(0,true,true);
        for(int i=0;i<P04HF13Recovery.LIMIT;i++)ok(trueValue(exact(0,0xdf82522e,Boolean.FALSE)),
            "within changed-call cap #"+i);
        ok(!P04HF13Recovery.isEnabled(),"limit auto OFF");
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"after limit original result");
        setup();P04HF13Recovery.arm(0,true,true);
        P04ExactGateTrial.setDeoptimized(false);
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"deoptimization loss fail closed");
        setup();P04HF13Recovery.arm(0,true,true);
        frame(0,0,0);
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"A contradictory native true revokes");
        setup();P04HF13Recovery.arm(0,true,true);
        P04HF13Recovery.onHookFailure();
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"hook installation failure revokes");
        setup();frame(1,0,1);frame(1,1,0);
        ok(!P04ExactGateTrial.eligibleForHF13Recovery(),"B evidence if present must fully qualify");
        setup();P04ExactGateTrial.onCaptureStart(0);
        ok(!P04ExactGateTrial.eligibleForHF13Recovery(),"no stale A eligibility on new recording");
        for(int i=0;i<5;i++)frame(0,1,0);
        ok(P04ExactGateTrial.eligibleForHF13Recovery(),"fresh five A observations requalify");
        P04ExactGateTrial.onCaptureStop();
        ok(!P04ExactGateTrial.eligibleForHF13Recovery(),"Stop A revokes fresh window");
        P04ExactGateTrial.onCaptureStart(1);
        ok(!P04ExactGateTrial.eligibleForHF13Recovery(),"Start B revokes A window");
        setup();P04HF13Recovery.arm(0,true,true);
        java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger modifications=new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        Thread[] workers=new Thread[12];
        for(int i=0;i<workers.length;i++){
            workers[i]=new Thread(()->{try {gate.await();for(int j=0;j<16;j++){
                if(trueValue(exact(0,0xdf82522e,Boolean.FALSE)))modifications.incrementAndGet();
            }}catch(Throwable e){error.compareAndSet(null,e);}});
            workers[i].start();
        }
        gate.countDown();for(Thread th:workers)th.join();
        ok(error.get()==null,"parallel callbacks safe");
        ok(modifications.get()==P04HF13Recovery.LIMIT,"strict global cap under concurrency");
        ok(!P04HF13Recovery.isEnabled(),"concurrent exhaustion OFF");
        P04HF13Recovery.off("user disabled");
        ok(falseValue(exact(0,0xdf82522e,Boolean.FALSE)),"explicit OFF reversible");
        P04HF13Recovery.restoreClockForTest();
        System.out.println("ITM_HF13_EXACT_REELS_RECOVERY_SAFETY_NEGATIVES_PASS="+checks);
    }
}
