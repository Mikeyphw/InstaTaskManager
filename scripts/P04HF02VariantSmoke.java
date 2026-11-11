package com.mikeyphw.instataskmanager;
/** Exact-shape reflection tests using synthetic JVM-named X.09... classes. */
public final class P04HF02VariantSmoke {
    static int n;
    static void ck(boolean cond,String what) {if(!cond)throw new AssertionError(what);n++;}
    static Object fresh(String name) throws Exception {
        return Class.forName(name).getDeclaredConstructor().newInstance();
    }
    static void field(Object obj,String name,Object value) throws Exception {
        java.lang.reflect.Field f=obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        if(value instanceof Integer)f.setInt(obj,(Integer)value);
        else if(value instanceof Boolean)f.setBoolean(obj,(Boolean)value);
        else f.set(obj,value);
    }
    public static void main(String[] args) throws Exception {
        Object ua=fresh("X.09j4"), ub=fresh("X.09ZC");
        Object fa=fresh("X.09Xx"),fb=fresh("X.09YD");
        P04VariantShape.Observation a=P04VariantShape.inspect(4,ua);
        ck(a.variant==P04VariantShape.Observation.SINGLETON,"UI singleton verified");
        a=P04VariantShape.inspect(11,fa);
        ck(a.variant==P04VariantShape.Observation.SINGLETON,"UFI singleton verified");
        field(ub,"A00",5);
        field(ub,"A01",fresh("X.00w8"));
        field(ub,"A03","SECRET_ACCOUNT_NEVER_EXPORTED");
        field(ub,"A06",true);
        field(ub,"A07",false);
        a=P04VariantShape.inspect(4,ub);
        ck(a.variant==P04VariantShape.Observation.PAYLOAD,"UI data variant verified");
        ck(a.integerSign==1,"int bucket positive");
        ck(a.uiFlag06 && !a.uiFlag07,"boolean flags exact");
        ck(a.presentReferences==1 && a.presentStrings==1,"only reference presence counted");
        field(fb,"A00",-3);
        a=P04VariantShape.inspect(11,fb);
        ck(a.variant==P04VariantShape.Observation.PAYLOAD && a.integerSign==-1,"UFI int negative");
        ck(P04VariantShape.inspect(12,ub).variant==P04VariantShape.Observation.UNKNOWN,"unknown selector excluded");
        ck(P04VariantShape.inspect(4,new Object()).variant==P04VariantShape.Observation.UNKNOWN,"unknown class excluded");
        ck(P04VariantShape.inspect(4,null).variant==P04VariantShape.Observation.UNKNOWN,"null ignored");
        P04TraceSummary sum=new P04TraceSummary();
        sum.start(0);sum.record(4,ua);sum.record(11,fa);sum.stop();
        sum.start(1);sum.record(4,ub);sum.record(11,fb);sum.stop();
        String report=sum.render();
        ck(report.contains("singleton=1") && report.contains("payload=1"),"per-cohort variant evidence");
        ck(report.contains("A06(true/false)=1/0") && report.contains("A07(true/false)=0/1"),"primitive aggregates");
        ck(report.contains("references=1 strings=1"),"reference presence only");
        ck(report.contains("positive=1") && report.contains("negative=1"),"number sign aggregation");
        ck(!report.contains("SECRET_ACCOUNT_NEVER_EXPORTED"),"no strings leaked");
        ck(!sum.booleanGapReady(),"variant A/B cannot trigger scalar boolean override");
        System.out.println("ITM_P04_HF02_VARIANT_SMOKE_PASS="+n);
    }
}
