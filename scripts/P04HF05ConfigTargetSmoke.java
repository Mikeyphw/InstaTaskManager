package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;

public final class P04HF05ConfigTargetSmoke {
    static int passed=0;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);passed++;}
    interface Mobile {boolean BdA(long key);}
    interface Factory { }
    static final class Correct implements Mobile,Factory {
        public boolean BdA(long k){return k!=0;}
    }
    static final class MissingFactory implements Mobile {
        public boolean BdA(long k){return false;}
    }
    static final class WrongReturn implements Mobile,Factory {
        public boolean BdA(long k){return false;}
        public Boolean BdA(Integer k){return Boolean.FALSE;}
    }
    static final class WrongSignature implements Mobile,Factory {
        public boolean BdA(long k){return false;}
        public boolean BdA(int k){return true;}
    }
    static final class WrongName implements Mobile,Factory {
        public boolean BdA(long k){return true;}
        public boolean wrong(long k){return true;}
    }
    abstract static class AbstractReceiver implements Mobile, Factory { }
    static class PackagePrivateMethod implements Mobile,Factory {
        public boolean BdA(long key){return true;}
        boolean other(long key){return true;}
    }
    interface InvalidFactory {}
    interface InvalidConfig {boolean BdA(long key);}
    static final class NoMethod implements InvalidFactory, InvalidConfig {
        public boolean BdA(long k){return true;}
    }
    static void refuses(Class<?> receiver,Class<?> a,Class<?> b) {
        try {P04ConfigTarget.verify(receiver,a,b);throw new AssertionError("must refuse "+receiver);}
        catch(NoSuchMethodException expected){passed++;}
    }
    public static void main(String[] args) throws Exception {
        Method method=P04ConfigTarget.verify(Correct.class,Mobile.class,Factory.class);
        check(method.getName().equals("BdA"),"correct method");
        check(method.getReturnType()==boolean.class,"scalar result");
        check(method.getParameterCount()==1,"one long argument");
        check(!java.lang.reflect.Modifier.isStatic(method.getModifiers()),"nonstatic");
        refuses(MissingFactory.class,Mobile.class,Factory.class);
        refuses(Correct.class,Mobile.class,InvalidFactory.class);
        refuses(Correct.class,InvalidConfig.class,Factory.class);
        refuses(AbstractReceiver.class,Mobile.class,Factory.class);
        refuses(Mobile.class,Mobile.class,Factory.class);
        refuses(NoMethod.class,Mobile.class,Factory.class);
        refuses(null,Mobile.class,Factory.class);
        check(P04ConfigTarget.verify(WrongReturn.class,Mobile.class,Factory.class)!=null,"overload doesn't override signature");
        check(P04ConfigTarget.verify(WrongSignature.class,Mobile.class,Factory.class)!=null,"int overload ignored");
        check(P04ConfigTarget.verify(WrongName.class,Mobile.class,Factory.class)!=null,"unrelated name ignored");
        check(P04ConfigTarget.verify(PackagePrivateMethod.class,Mobile.class,Factory.class)!=null,"public target accepted");
        System.out.println("ITM_P04_HF05_CONCRETE_CONFIG_SHAPE_PASS="+passed);
    }
}
