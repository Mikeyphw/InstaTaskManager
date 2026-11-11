package com.mikeyphw.instataskmanager;

import java.lang.reflect.Field;
/** Pure-Java input probe and A/B outcome-correlation smoke. */
public final class P04HF04InputSmoke {
    public static final class Wrapper { public Object A02; }
    public static final class Model { public Object A0u; }
    static void ok(boolean pass,String label) {if(!pass)throw new AssertionError(label);}
    public static void main(String[] args) throws Exception {
        Field outer=Wrapper.class.getDeclaredField("A02");
        Field inner=Model.class.getDeclaredField("A0u");
        P04InputPresence probe=new P04InputPresence(outer,inner,Model.class);
        Wrapper w=new Wrapper();
        ok(probe.classify(w)==P04InputPresence.OUTER_NULL,"null parent");
        w.A02=new Model();
        ok(probe.classify(w)==P04InputPresence.NESTED_NULL,"null nested");
        ((Model)w.A02).A0u=new Object();
        ok(probe.classify(w)==P04InputPresence.NESTED_PRESENT,"present nested");
        w.A02=new Object();
        ok(probe.classify(w)==P04InputPresence.UNAVAILABLE,"mismatched owner");
        ok(probe.classify(null)==P04InputPresence.UNAVAILABLE,"null hook");
        P04PredicateTrace.clear();
        P04PredicateTrace.Frame fa=P04PredicateTrace.enter(0,P04InputPresence.NESTED_NULL);
        P04PredicateTrace.leave(fa,new Object());
        P04PredicateTrace.Frame fb=P04PredicateTrace.enter(1,P04InputPresence.NESTED_PRESENT);
        P04PredicateTrace.leave(fb,new Object());
        String r=P04PredicateTrace.report();
        ok(r.contains("A0u-null=1") && r.contains("A0u-present=1"),"A/B counted");
        ok(!r.contains("java.lang.Object@"),"no object strings");
        P04PredicateTrace.clear();
        r=P04PredicateTrace.report();
        ok(!r.contains("A0u-null=1") && !r.contains("A0u-present=1"),"clear");
        System.out.println("ITM_P04_HF04_INPUT_PRESENCE_AND_AB_COUNTERS_PASS");
    }
}
