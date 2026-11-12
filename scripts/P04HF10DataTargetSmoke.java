package com.mikeyphw.instataskmanager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class P04HF10DataTargetSmoke {
    public interface Contract { Boolean D3r(int key); }
    public static final class Provider implements Contract {
        public int calls;
        public Boolean D3r(int key) { calls++; return Boolean.TRUE; }
    }
    public static abstract class AbstractProvider implements Contract {
        public abstract Boolean D3r(int key);
    }
    public static final class WrongProvider implements Contract {
        public Boolean D3r(int key) { return null; }
        // Since it implements Contract, it isn't wrong. It should qualify.
    }
    public static final class Lambda { public Object A03; }
    public static final class Data { public Contract A01; }
    public static final class NonpublicData { private Contract A01; }
    public static final class WrongType { public String A01; }
    private static int n;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);n++;}
    public static void main(String[] args) throws Exception {
        Field outer=Lambda.class.getDeclaredField("A03");
        Field inner=Data.class.getDeclaredField("A01");
        check(P04ActualDataTarget.validFields(outer,inner,Data.class,Contract.class),"correct field structure");
        check(!P04ActualDataTarget.validFields(outer,NonpublicData.class.getDeclaredField("A01"),NonpublicData.class,Contract.class),"private field rejected");
        check(!P04ActualDataTarget.validFields(outer,WrongType.class.getDeclaredField("A01"),WrongType.class,Contract.class),"wrong field rejected");
        check(!P04ActualDataTarget.validFields(null,inner,Data.class,Contract.class),"missing field rejected");
        P04ActualDataTarget target=new P04ActualDataTarget(outer,inner,Data.class,Contract.class);
        Lambda lambda=new Lambda();
        check(target.resolve(lambda)==null,"no data no hook");
        Data data=new Data();lambda.A03=data;
        check(target.resolve(lambda)==null,"no provider no hook");
        Provider provider=new Provider();data.A01=provider;
        Method method=target.resolve(lambda);
        check(method!=null,"real provider hookable");
        check(method.getDeclaringClass()==Provider.class,"resolves concrete owner");
        check(method.getName().equals("D3r") && method.getParameterCount()==1 && method.getReturnType()==Boolean.class,"exact boxed shape");
        check(provider.calls==0,"resolver cannot invoke provider");
        check(P04ActualDataTarget.verifiedMethod(AbstractProvider.class,Contract.class)==null,"abstract owner refused");
        check(P04ActualDataTarget.verifiedMethod(String.class,Contract.class)==null,"unrelated owner refused");
        check(P04ActualDataTarget.verifiedMethod(WrongProvider.class,Contract.class)!=null,"valid concrete null-return method accepted");
        check(target.resolve(new Object())==null,"foreign lambda refused");
        System.out.println("ITM_HF10_ACTUAL_REPOST_DATA_PROVIDER_PASS="+n);
    }
}
