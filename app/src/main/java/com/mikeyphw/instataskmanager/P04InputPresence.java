package com.mikeyphw.instataskmanager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Exact IG455 input-presence probe for X.0Ie0 selector 4.
 * The DEX reads X.0Ie0.A02 as X.07XN and then reads its A0u field;
 * if that field is null, execution can return the X.09j4 singleton early.
 * Do NOT read, retain, stringify, or replace the referenced object.
 */
final class P04InputPresence {
    static final int OUTER_NULL = 0;
    static final int NESTED_NULL = 1;
    static final int NESTED_PRESENT = 2;
    static final int UNAVAILABLE = 3;
    static final String[] LABELS = {"A02-null", "A0u-null", "A0u-present", "unavailable"};
    private final Field outer;
    private final Field nested;
    private final Class<?> expectedOuter;

    P04InputPresence(Field outer, Field nested, Class<?> expectedOuter) {
        this.outer=outer;this.nested=nested;this.expectedOuter=expectedOuter;
    }
    static P04InputPresence preflight(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> owner=Class.forName("X.0Ie0",false,loader);
        Class<?> model=Class.forName("X.07XN",false,loader);
        Class<?> nestedType=Class.forName("X.00w8",false,loader);
        Field outer=owner.getDeclaredField("A02");
        Field nested=model.getDeclaredField("A0u");
        if (outer.getType()!=Object.class || nested.getType()!=nestedType ||
            !Modifier.isPublic(outer.getModifiers()) ||
            !Modifier.isPublic(nested.getModifiers()))
            throw new NoSuchFieldException("Input shape mismatch");
        return new P04InputPresence(outer,nested,model);
    }
    int classify(Object hookInstance) {
        if (hookInstance==null)return UNAVAILABLE;
        try {
            Object model=outer.get(hookInstance);
            if (model==null)return OUTER_NULL;
            if (!expectedOuter.isInstance(model))return UNAVAILABLE;
            return nested.get(model)==null?NESTED_NULL:NESTED_PRESENT;
        } catch (Throwable ignored) {return UNAVAILABLE;}
    }
}
