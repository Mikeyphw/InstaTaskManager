package com.mikeyphw.instataskmanager;

public final class SocialShapeSmoke {
    static class Example {
        public Object invoke() { return null; }
        public Object invoke(Object arg) { return arg; }
        public void post(int a, Object b) { }
        public void post(long a, Object b) { }
        public String stringValue() { return "x"; }
    }
    static void expect(String wanted, SocialMethodShape.Match actual) {
        if (!wanted.equals(actual.state)) throw new AssertionError(wanted + " vs " + actual.state);
    }
    public static void main(String[] args) {
        expect("STRUCTURE_FOUND", SocialMethodShape.examine(Example.class, "invoke", 0, "java.lang.Object"));
        expect("STRUCTURE_FOUND", SocialMethodShape.examine(Example.class, "invoke", 1, "java.lang.Object"));
        expect("AMBIGUOUS", SocialMethodShape.examine(Example.class, "invoke", -1, "java.lang.Object"));
        expect("AMBIGUOUS", SocialMethodShape.examine(Example.class, "post", 2, "void"));
        expect("NOT_FOUND", SocialMethodShape.examine(Example.class, "post", 2, "java.lang.Object"));
        expect("NOT_FOUND", SocialMethodShape.examine(Example.class, "missing", -1, "*"));
        expect("STRUCTURE_FOUND", SocialMethodShape.examine(Example.class, "stringValue", 0, "java.lang.String"));
        expect("UNAVAILABLE", SocialMethodShape.examine(null, "x", 0, "*"));
        System.out.println("ITM_P01_SOCIAL_SHAPE_SMOKE_PASS 8/8");
    }
}
