package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * HF05 exact-IG455 MobileConfig dispatch-target verification.
 * DEX evidence: X.01ym.A02() returns X.0mMk, whose concrete X.0T4C also
 * implements MobileConfigUnsafeContext. The previous X.02ja hook did not
 * intercept the initial Repost predicate in live A/B captures.
 * IMPORTANT: X.0T4C is abstract at CLASS level (DEX flags 0x401),
 * but declares public concrete BdA(J)Z (method flags 0x11).
 * Do not reject the owner for abstractness; only reject an abstract METHOD.
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
                // An abstract *owner* is valid if the specific declared BdA(long)
                // method is concrete. IG455 X.0T4C has class flags 0x401,
                // but BdA(J)Z itself has flags 0x11 and executable code.
                provider.isInterface() ||
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
