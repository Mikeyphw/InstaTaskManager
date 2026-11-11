package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * HF05 exact-IG455 MobileConfig dispatch-target verification.
 * DEX evidence: X.01ym.A02() returns X.0mMk, whose concrete X.0T4C also
 * implements MobileConfigUnsafeContext. The previous X.02ja hook did not
 * intercept the initial Repost predicate in live A/B captures.
 * This does not invoke the method or read its arguments or configuration keys.
 */
final class P04ConfigTarget {
    private P04ConfigTarget() { }

    static Method resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> provider=Class.forName("X.0T4C",false,loader);
        Class<?> config=Class.forName("com.facebook.mobileconfig.factory.MobileConfigUnsafeContext",false,loader);
        Class<?> factory=Class.forName("X.0mMk",false,loader);
        return verify(provider,config,factory);
    }

    static Method verify(Class<?> provider,Class<?> config,Class<?> factory)
            throws NoSuchMethodException {
        if (provider == null || config == null || factory == null ||
                provider.isInterface() || Modifier.isAbstract(provider.getModifiers()) ||
                !config.isAssignableFrom(provider) || !factory.isAssignableFrom(provider))
            throw new NoSuchMethodException("HF05 MobileConfig receiver shape mismatch");
        Method method=provider.getDeclaredMethod("BdA",long.class);
        int m=method.getModifiers();
        if(method.getReturnType()!=boolean.class || Modifier.isAbstract(m) ||
                Modifier.isStatic(m) || !Modifier.isPublic(m))
            throw new NoSuchMethodException("HF05 MobileConfig method shape mismatch");
        return method;
    }
}
