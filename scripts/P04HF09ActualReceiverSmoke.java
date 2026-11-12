package com.mikeyphw.instataskmanager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Pure Java contract: the observed receiver is concrete and delegates BdA. */
public final class P04HF09ActualReceiverSmoke {
    interface Factory { boolean BdA(long key); }
    static final class Actual implements Factory {
        public boolean BdA(long key){ return key != 0L; }
    }
    static final class MissingFactory {public boolean BdA(long key){return false;}}
    static final class NoMethod implements Factory {public boolean BdA(long key){return false;}}
    static final class WrongParams implements Factory {
        public boolean BdA(long key){return false;}
        public boolean BdA(int key){return false;}
    }
    abstract static class AbstractMethod implements Factory { public abstract boolean BdA(long key); }
    interface Unrelated { }
    private static int passed;
    static void ok(boolean b,String reason){if(!b)throw new AssertionError(reason);passed++;}
    static void refuse(Class<?> c,Class<?> expected){
        try {P04ActualConfigTarget.verify(c,expected);throw new AssertionError("Accepted invalid "+c);}
        catch(NoSuchMethodException expectedError){passed++;}
    }
    public static void main(String[] args) throws Exception {
        Method m=P04ActualConfigTarget.verify(Actual.class,Factory.class);
        ok(m.getDeclaringClass()==Actual.class,"declares own code-bearing method");
        ok(m.getParameterCount()==1 && m.getParameterTypes()[0]==long.class,"BdA(J)");
        ok(m.getReturnType()==boolean.class,"Boolean primitive return");
        ok(!Modifier.isStatic(m.getModifiers()) && !Modifier.isAbstract(m.getModifiers()),"concrete instance");
        refuse(MissingFactory.class,Factory.class);
        refuse(Actual.class,Unrelated.class);
        refuse(AbstractMethod.class,Factory.class);
        refuse(Factory.class,Factory.class);
        refuse(null,Factory.class);
        ok(P04ActualConfigTarget.verify(WrongParams.class,Factory.class)!=null,"only one exact overload matched");
        ok(!P04PredicateTrace.inRepostFrame(),"global calls unobserved by default");
        P04PredicateTrace.Frame frame=P04PredicateTrace.enter(0,P04InputPresence.NESTED_PRESENT);
        ok(P04PredicateTrace.inRepostFrame(),"only manual active selector-4 frame observed");
        P04PredicateTrace.leave(frame,null);
        ok(!P04PredicateTrace.inRepostFrame(),"frame restored on exit");
        ok(P04PredicateTrace.classifyFactoryName("X.02nr")==2,"actual provider classified distinctly");
        ok(P04PredicateTrace.classifyFactoryName("X.02ja")==0,"historical class preserved");
        ok(P04PredicateTrace.classifyFactoryName("X.0T4C")==1,"historical abstract class preserved");
        ok(P04PredicateTrace.classifyFactoryName("X.09ZZ")==3,"other X subclass metadata");
        ok(P04PredicateTrace.classifyFactoryName("java.lang.String")==5,"no arbitrary class metadata");
        System.out.println("ITM_HF09_ACTUAL_RECEIVER_AND_FACTORY_CLASSIFICATION_PASS="+passed);
    }
}
