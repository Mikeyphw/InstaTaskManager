package com.mikeyphw.instataskmanager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Exact IG455 (base SHA-256 fb6ca8c2...) structural fingerprints only.
 * Verified in classes5.dex: selector 4 returns variants implementing X.0Nhe;
 * selector 11 returns variants implementing X.0Ngy.
 * NO object/string values, IDs, media or account metadata are retained.
 */
final class P04VariantShape {
    static final class Observation {
        static final int UNKNOWN=0, SINGLETON=1, PAYLOAD=2, SHAPE_MISMATCH=3;
        final int variant, integerSign, presentReferences, presentStrings;
        final boolean hasUiFlags, uiFlag06, uiFlag07;
        Observation(int v, int sign, int refs, int strings,
                    boolean hasFlags, boolean flag06, boolean flag07) {
            variant=v; integerSign=sign; presentReferences=refs;
            presentStrings=strings; hasUiFlags=hasFlags;
            uiFlag06=flag06; uiFlag07=flag07;
        }
    }
    private P04VariantShape() { }
    private static Observation result(int variant, int sign, int refs, int strings,
                                      boolean hasFlags, boolean flag06, boolean flag07) {
        return new Observation(variant,sign,refs,strings,hasFlags,flag06,flag07);
    }
    private static Observation unknown() {return result(Observation.UNKNOWN,0,0,0,false,false,false);}
    private static Observation invalid() {return result(Observation.SHAPE_MISMATCH,0,0,0,false,false,false);}
    private static Field required(Class<?> owner,String name,String type) throws ReflectiveOperationException {
        Field f=owner.getDeclaredField(name);
        if (!Modifier.isPublic(f.getModifiers()) || !f.getType().getName().equals(type))
            throw new NoSuchFieldException("IG455 shape changed");
        return f;
    }
    private static int sign(int value) {return Integer.compare(value,0);}
    private static boolean implementsExact(Class<?> owner,String interfaceName) {
        for(Class<?> item:owner.getInterfaces())
            if(interfaceName.equals(item.getName())) return true;
        return false;
    }
    /** Observe only named, exact-type return objects; never call methods on an Instagram object. */
    static Observation inspect(int selector,Object value) {
        if (value==null) return unknown();
        Class<?> owner=value.getClass();
        String name=owner.getName();
        if (selector==4) {
            if (("X.09j4".equals(name) || "X.09ZC".equals(name))
                    && !implementsExact(owner,"X.0Nhe")) return invalid();
            if ("X.09j4".equals(name)) {
                try {
                    Field singleton=required(owner,"A00",name);
                    if (!Modifier.isStatic(singleton.getModifiers()) ||
                        !Modifier.isFinal(singleton.getModifiers())) return invalid();
                    return result(Observation.SINGLETON,0,0,0,false,false,false);
                } catch (ReflectiveOperationException | RuntimeException e) {return invalid();}
            }
            if ("X.09ZC".equals(name)) {
                try {
                    Field number=required(owner,"A00","int");
                    Field ref1=required(owner,"A01","X.00w8");
                    Field ref2=required(owner,"A02","X.09ZB");
                    Field s1=required(owner,"A03","java.lang.String");
                    Field s2=required(owner,"A04","java.lang.String");
                    Field s3=required(owner,"A05","java.lang.String");
                    Field f6=required(owner,"A06","boolean");
                    Field f7=required(owner,"A07","boolean");
                    // Presence only: never stringify, serialize or retain references.
                    int refs=(ref1.get(value)!=null?1:0)+(ref2.get(value)!=null?1:0);
                    int strings=(s1.get(value)!=null?1:0)+(s2.get(value)!=null?1:0)
                                +(s3.get(value)!=null?1:0);
                    return result(Observation.PAYLOAD,sign(number.getInt(value)),refs,strings,
                                  true,f6.getBoolean(value),f7.getBoolean(value));
                } catch (ReflectiveOperationException | RuntimeException e) {return invalid();}
            }
        } else if (selector==11) {
            if (("X.09Xx".equals(name) || "X.09YD".equals(name))
                    && !implementsExact(owner,"X.0Ngy")) return invalid();
            if ("X.09Xx".equals(name)) {
                try {
                    Field singleton=required(owner,"A00",name);
                    if (!Modifier.isStatic(singleton.getModifiers()) ||
                        !Modifier.isFinal(singleton.getModifiers())) return invalid();
                    return result(Observation.SINGLETON,0,0,0,false,false,false);
                } catch (ReflectiveOperationException | RuntimeException e) {return invalid();}
            }
            if ("X.09YD".equals(name)) {
                try {
                    Field number=required(owner,"A00","int");
                    return result(Observation.PAYLOAD,sign(number.getInt(value)),0,0,false,false,false);
                } catch (ReflectiveOperationException | RuntimeException e) {return invalid();}
            }
        }
        return unknown();
    }
}
