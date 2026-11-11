package com.mikeyphw.instataskmanager;
public final class P04TraceSummarySmoke {
    public static final class FakeUi { public final int $t; FakeUi(int v){$t=v;} }
    private static int count;
    static void check(boolean ok,String what) {
        if (!ok)throw new AssertionError(what);
        count++;
    }
    public static void main(String[] args) throws Exception {
        P04TraceSummary s=new P04TraceSummary();
        check(!s.recording(),"default off");
        check(s.feedback().contains("READY"),"ready feedback");
        check(s.activeIndex()==-1,"not active on startup");
        check(s.start(-1)==-1,"invalid -1");
        check(s.start(2)==-1,"invalid 2");
        check(s.start(0)==0,"start A");
        check(s.recording(),"recording A");
        check(s.feedback().contains("RECORDING A"),"A feedback visible");
        check(s.feedback().contains("00:00"),"elapsed-time display");
        check(s.feedback().contains("0 gate events"),"zero-event feedback explicit");
        check(s.activeIndex()==0,"capture A status index");
        s.record(4,Boolean.FALSE);
        s.record(4,Boolean.TRUE);
        s.record(11,null);
        s.record(12,new Object());
        s.record(5555,Boolean.FALSE);
        check(s.feedback().contains("4 gate events"),"live counter reflects all selectors");
        String a=s.render();
        check(a.contains("true=1 false=1"),"boolean tally A");
        check(a.contains("null=1"),"null tally A");
        check(a.contains("object=1"),"nonboolean tally A");
        check(a.contains("type=java.lang.Object"),"type-only metadata");
        check(!a.contains("5555"),"unknown selector skipped");
        s.stop();
        check(!s.recording(),"stop");
        check(s.activeIndex()==-1,"stop clears active cohort");
        check(s.feedback().contains("STOPPED"),"stopped feedback visible");
        check(s.start(1)==1,"start B");
        check(s.feedback().contains("RECORDING B"),"B feedback visible");
        s.record(4,Boolean.TRUE);
        s.uiCheck("visited=5 repost_labels=1 friends_labels=0");
        String b=s.render();
        check(b.contains("ACCOUNT B"),"B label");
        check(b.contains("visited=5 repost_labels=1"),"UI counts");
        check(b.contains("true=1 false=1"),"A preserved");
        for(int i=0;i<300;i++)s.record(11,Boolean.TRUE);
        check(s.render().contains("samples=256") && s.render().contains("capped_extra=44"),"bounded counters");
        s.clear();
        check(!s.recording(),"clear disarms");
        check(s.feedback().contains("READY"),"clear resets feedback");
        check(s.render().contains("samples=0"),"clear empties");
        check(s.render().contains("Not checked"),"clear UI counts");
        s.start(0);
        for(int i=0;i<3;i++)s.record(4,Boolean.FALSE);
        s.start(1);
        for(int i=0;i<3;i++)s.record(4,Boolean.TRUE);
        check(s.booleanGapReady(),"strict A/B boolean gap");
        s.start(0);
        check(s.activeA(),"trial only in cohort A");
        s.record(4,null);
        check(!s.booleanGapReady(),"ambiguous null invalidates gate");
        // Entire P04 guarded scalar override is tested without Android/Instagram.
        java.lang.reflect.Field selector=P04GateTrace.class.getDeclaredField("uiSelector");
        selector.setAccessible(true);
        selector.set(null,FakeUi.class.getDeclaredField("$t"));
        P04GateTrace.installed();
        check(P04GateTrace.begin(0).contains("recording"),"Begin A requires installed hooks");
        for(int i=0;i<3;i++) check(Boolean.FALSE.equals(P04GateTrace.noteUi(new FakeUi(4),Boolean.FALSE)),"passive A original");
        check(P04GateTrace.begin(1).contains("recording"),"Begin B");
        for(int i=0;i<3;i++) check(Boolean.TRUE.equals(P04GateTrace.noteUi(new FakeUi(4),Boolean.TRUE)),"passive B original");
        check(P04GateTrace.begin(0).contains("recording"),"Begin A trial");
        check(P04GateTrace.enableBooleanTrial().contains("ARMED"),"eligible scalar trial armed");
        check(Boolean.FALSE.equals(P04GateTrace.noteUi(new FakeUi(3),Boolean.FALSE)),"other selector untouched");
        check(P04GateTrace.noteUi(new FakeUi(3),new Object()).getClass()==Object.class,"non-Boolean untouched");
        for(int i=0;i<8;i++)check(Boolean.TRUE.equals(P04GateTrace.noteUi(new FakeUi(4),Boolean.FALSE)),"bounded trial one of eight");
        check(Boolean.FALSE.equals(P04GateTrace.noteUi(new FakeUi(4),Boolean.FALSE)),"trial auto-disarmed");
        check(P04GateTrace.report().contains("replacements=8/8"),"bounded trial report");
        check(P04GateTrace.stop().contains("trial OFF"),"stop disarms");
        check(Boolean.FALSE.equals(P04GateTrace.noteUi(new FakeUi(4),Boolean.FALSE)),"stopped passive");
        System.out.println("ITM_P04_AB_SUMMARY_SMOKE_PASS "+count+"/"+count);
    }
}
