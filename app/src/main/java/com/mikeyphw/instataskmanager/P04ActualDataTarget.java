package com.mikeyphw.instataskmanager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * HF10: resolve the ACTUAL X.0ApG implementation used by the Repost UI data.
 * Source: IG455 X.0Ie0.invoke($t=4) when first BdA gate is false:
 *   X.0Ie0.A03 -> X.0FWn.A01 (X.0ApG) -> D3r(0xdf82522e).
 * Only inspects Java class and public fields; does not call D3r, touch account
 * identity/content, emit object values or obtain any network data.
 */
final class P04ActualDataTarget {
    private final Field repost;
    private final Field provider;
    private final Class<?> repostType;
    private final Class<?> providerContract;

    P04ActualDataTarget(Field repost, Field provider, Class<?> repostType, Class<?> providerContract) {
        this.repost=repost;
        this.provider=provider;
        this.repostType=repostType;
        this.providerContract=providerContract;
    }

    static P04ActualDataTarget preflight(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> owner=Class.forName("X.0Ie0",false,loader);
        Class<?> data=Class.forName("X.0FWn",false,loader);
        Class<?> contract=Class.forName("X.0ApG",false,loader);
        Field outer=owner.getDeclaredField("A03");
        Field inner=data.getDeclaredField("A01");
        if (!validFields(outer,inner,data,contract))
            throw new NoSuchFieldException("HF10 exact Repost-data field contract changed");
        return new P04ActualDataTarget(outer,inner,data,contract);
    }

    static boolean validFields(Field outer,Field inner,Class<?> data,Class<?> contract) {
        if(outer==null || inner==null || data==null || contract==null)return false;
        return outer.getType()==Object.class && inner.getType()==contract &&
               Modifier.isPublic(outer.getModifiers()) &&
               Modifier.isPublic(inner.getModifiers()) &&
               !Modifier.isStatic(outer.getModifiers()) &&
               !Modifier.isStatic(inner.getModifiers()) &&
               data.isAssignableFrom(inner.getDeclaringClass());
    }

    Method resolve(Object lambdaInstance) {
        if(lambdaInstance==null)return null;
        try {
            Object data=repost.get(lambdaInstance);
            if(data==null || !repostType.isInstance(data))return null;
            Object actual=provider.get(data);
            if(actual==null || !providerContract.isInstance(actual))return null;
            // Does not invoke Instagram code: only the already-created object's type.
            return verifiedMethod(actual.getClass(),providerContract);
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }

    static Method verifiedMethod(Class<?> actual,Class<?> contract) {
        if(actual==null || contract==null || !contract.isAssignableFrom(actual))return null;
        try {
            Method candidate=actual.getMethod("D3r",int.class);
            int modifiers=candidate.getModifiers();
            if(candidate.getReturnType()!=Boolean.class ||
               Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers) ||
               !Modifier.isPublic(modifiers) ||
               candidate.getDeclaringClass().isInterface())return null;
            return candidate;
        } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
