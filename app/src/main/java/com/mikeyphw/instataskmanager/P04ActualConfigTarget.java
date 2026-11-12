package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * HF09: The actual result of X.01ym.A02(...) in BOTH IG455 account cohorts
 * was X.02nr, not either historically guessed X.02ja / X.0T4C owner.
 * Static exact APK classes.dex: LX/02nr; is public/final (flags 0x11),
 * declares public/final BdA(J)Z (flags 0x11) WITH CODE at 0x546b28.
 * Never invokes a configuration method to discover its target and never
 * reads/configures any account, key, media or raw result outside a bounded
 * original selector-4 frame.
 */
final class P04ActualConfigTarget {
    private P04ActualConfigTarget() { }

    static Method resolve(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> provider=Class.forName("X.02nr",false,loader);
        Class<?> factoryContract=Class.forName("X.0mMk",false,loader);
        return verify(provider,factoryContract);
    }

    static Method verify(Class<?> provider,Class<?> factoryContract)
            throws NoSuchMethodException {
        if (provider==null || factoryContract==null || provider.isInterface() ||
                !factoryContract.isAssignableFrom(provider))
            throw new NoSuchMethodException("HF09 actual factory receiver type mismatch");
        Method method=provider.getDeclaredMethod("BdA",long.class);
        int m=method.getModifiers();
        if(method.getReturnType()!=boolean.class || Modifier.isAbstract(m) ||
                Modifier.isStatic(m) || !Modifier.isPublic(m))
            throw new NoSuchMethodException("HF09 actual receiver method shape mismatch");
        return method;
    }
}
