package com.mikeyphw.instataskmanager;

public final class P04HF03PredicateSmoke {
    static int n;
    static void ok(boolean v,String what){if(!v)throw new AssertionError(what);n++;}
    static final class Secret {final String token="private account secret";}
    public static void main(String[] args){
        P04PredicateTrace.clear();
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.TRUE);
        ok(!P04PredicateTrace.report().contains("true=1"),"no scope no record");
        P04PredicateTrace.Frame a=P04PredicateTrace.enter(0);
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.FALSE);
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.TRUE);
        P04PredicateTrace.note(P04PredicateTrace.ELIGIBILITY,null);
        P04PredicateTrace.note(P04PredicateTrace.SESSION_CHECK,Boolean.FALSE);
        P04PredicateTrace.Frame nested=P04PredicateTrace.enter(0);
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.FALSE);
        P04PredicateTrace.leave(nested,null);
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.TRUE);
        P04PredicateTrace.leave(a,new Secret());
        ok(P04PredicateTrace.report().contains("frames=2"),"frames correlated");
        P04PredicateTrace.Frame b=P04PredicateTrace.enter(1);
        for(int i=0;i<17;i++)P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.TRUE);
        P04PredicateTrace.note(P04PredicateTrace.ELIGIBILITY,Boolean.TRUE);
        P04PredicateTrace.leave(b,new Secret());
        String text=P04PredicateTrace.report();
        ok(text.contains("Manual account A")&&text.contains("Manual account B"),"two cohorts");
        ok(text.contains("#1: true=0 false=2") && text.contains("#1: true=1 false=0"),"first ordinal tracked");
        ok(text.contains("#2: true=1 false=0"),"second ordinal tracked");
        ok(text.contains("#3: true=1 false=0"),"third ordinal tracked");
        ok(!text.contains("#5:"),"bounded ordinals");
        ok(!text.contains("private account secret"),"no object values");
        ok(!text.contains("java.lang"),"no arbitrary class metadata");
        P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,Boolean.FALSE);
        ok(text.equals(P04PredicateTrace.report()),"no leakage after scope");
        P04PredicateTrace.clear();
        ok(!P04PredicateTrace.report().contains("frames="),"clear data");
        P04PredicateTrace.Frame bad=P04PredicateTrace.enter(-1);
        ok(bad==null,"invalid cohort refused");
        System.out.println("ITM_P04_HF03_PRIMITIVE_SCOPE_SMOKE_PASS="+n);
    }
}
