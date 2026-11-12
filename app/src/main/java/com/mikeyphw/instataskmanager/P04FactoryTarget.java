package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Exact IG455 factory invoked before the first MobileConfig.BdA gate.
 *  Static DEX: classes.dex X.01ym.A02(X.06g8, boolean):X.0mMk.
 *  The probe does not read its arguments, call it directly, or inspect the
 *  returned provider; it only classifies the already-returned Java class.
 */
final class P04FactoryTarget {
    private P04FactoryTarget() { }
    static Method resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> owner=Class.forName("X.01ym",false,loader);
        Class<?> arg=Class.forName("X.06g8",false,loader);
        Class<?> ret=Class.forName("X.0mMk",false,loader);
        return verify(owner,arg,ret);
    }
    static Method verify(Class<?> owner,Class<?> arg,Class<?> ret)
            throws NoSuchMethodException {
        if(owner==null || arg==null || ret==null || owner.isInterface())
            throw new NoSuchMethodException("HF07 factory type mismatch");
        Method method=owner.getDeclaredMethod("A02",arg,boolean.class);
        int modifiers=method.getModifiers();
        if(method.getReturnType()!=ret || !Modifier.isPublic(modifiers) ||
                !Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers))
            throw new NoSuchMethodException("HF07 factory method mismatch");
        return method;
    }
}
