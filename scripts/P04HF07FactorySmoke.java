package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;

public final class P04HF07FactorySmoke {
    static class Arg { }
    static class Ret { }
    static class Factory {
        public static Ret A02(Arg a, boolean b) { return null; }
    }
    static class WrongReturn {
        public static String A02(Arg a, boolean b) {return "bad";}
    }
    static class WrongStatic {
        public Ret A02(Arg a, boolean b) {return null;}
    }
    static class WrongSignature {
        public static Ret A02(Arg a) {return null;}
    }
    static void mustFail(Class<?> o) {
        try {P04FactoryTarget.verify(o,Arg.class,Ret.class);throw new AssertionError("must fail "+o);}
        catch (NoSuchMethodException expected) { }
    }
    static void ok(boolean v) { if(!v)throw new AssertionError("HF07 smoke fail"); }
    public static void main(String[] unused) throws Exception {
        Method m=P04FactoryTarget.verify(Factory.class,Arg.class,Ret.class);
        ok(java.lang.reflect.Modifier.isStatic(m.getModifiers()));
        mustFail(WrongReturn.class);
        mustFail(WrongStatic.class);
        mustFail(WrongSignature.class);
        P04PredicateTrace.clear();
        P04PredicateTrace.Frame a=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT);
        P04PredicateTrace.noteFactory(null);
        P04PredicateTrace.noteFactory(new Object());
        P04PredicateTrace.leave(a,null);
        P04PredicateTrace.Frame b=P04PredicateTrace.enter(1,P04InputPresence.NESTED_PRESENT);
        P04PredicateTrace.noteFactory(new Object());
        P04PredicateTrace.leave(b,null);
        String report=P04PredicateTrace.report();
        ok(report.contains("HF07 factory result #1"));
        ok(report.contains("NULL=1"));
        ok(report.contains("OTHER_PACKAGE=1"));
        ok(report.contains("HF07 factory result #2"));
        ok(!report.contains("java.lang.Object"));
        P04PredicateTrace.clear();
        ok(!P04PredicateTrace.report().contains("HF07 factory result #1"));
        System.out.println("ITM_P04_HF07_EXACT_FACTORY_SHAPE_AND_PRIVATE_COUNTS_PASS");
    }
}
