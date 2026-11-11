package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;

/** Pure Java structural verification; never invokes the target method. */
final class SocialMethodShape {
    private SocialMethodShape() { }

    static final class Match {
        final String state;
        final Method method;
        Match(String state, Method method) { this.state = state; this.method = method; }
    }

    /** Required return name is a Java VM binary class name or `*` for unspecified. */
    static Match examine(Class<?> cls, String methodName, int argumentCount, String returnName) {
        if (cls == null || methodName == null || returnName == null) return new Match("UNAVAILABLE", null);
        Method found = null;
        int matching = 0;
        for (Method method : cls.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (argumentCount >= 0 && method.getParameterCount() != argumentCount) continue;
            if (!"*".equals(returnName) && !method.getReturnType().getName().equals(returnName)) continue;
            matching++;
            if (matching == 1) found = method;
        }
        if (matching == 0) return new Match("NOT_FOUND", null);
        if (matching > 1) return new Match("AMBIGUOUS", null);
        return new Match("STRUCTURE_FOUND", found);
    }
}
