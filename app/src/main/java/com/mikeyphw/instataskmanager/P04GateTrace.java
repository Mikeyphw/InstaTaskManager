package com.mikeyphw.instataskmanager;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Locale;

/**
 * P04 exact-APK diagnostic only. No Instagram account identity is read or stored.
 * An observer may be installed in only two exact verified IG455 lambda methods.
 * Their selectors come from static IG455 DEX packed-switch analysis (not guesswork):
 *  X.0Ie0.invoke() $t=4 (RepostButtonUseCase getUiState/isEligible)
 *  X.0F2B.invoke(Object) $t=11,12 (UfiUseCase Repost button/count)
 * They are multipurpose. Passive A/B capture never changes return values.
 * HF03 disables the legacy Boolean trial; selectors return ORIGINAL values.
 * Primitive getter observations are captured only within selector 4 calls.
 */
final class P04GateTrace {
    private static final String IG_PACKAGE = "com.instagram.android";
    private static final String IG_VERSION = "455.0.0.8.72";
    private static final long IG_SIZE = 238346353L;
    private static final String IG_SHA256 = "fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6";
    private static final P04TraceSummary STATE = new P04TraceSummary();
    private static volatile String installStatus = "NOT REQUESTED (open Task Manager panel)";
    private static volatile boolean ready;
    private static Field uiSelector, ufiSelector;
    private static volatile P04InputPresence inputPresence;

    private P04GateTrace() { }
    static String status() {return installStatus;}
    static boolean ready() {return ready;}
    static int activeIndex() {return STATE.activeIndex();}
    static boolean recording() {return STATE.recording();}
    static String feedback() {
        if (!ready) return "P04 · " + installStatus;
        String capture=STATE.feedback();
        return capture;
    }
    static void setStatus(String status) {installStatus=status;}

    static String verifyExactApk(Context context) {
        try {
            if (context==null || !IG_PACKAGE.equals(context.getPackageName())) return "REFUSED: scope mismatch";
            PackageManager pm=context.getPackageManager();
            PackageInfo pkg=pm.getPackageInfo(IG_PACKAGE,0);
            if (!IG_VERSION.equals(pkg.versionName)) return "REFUSED: Instagram version not IG455";
            ApplicationInfo app=pm.getApplicationInfo(IG_PACKAGE,0);
            File apk=new File(app.sourceDir);
            if (!apk.isFile() || apk.length()!=IG_SIZE) return "REFUSED: IG455 APK length mismatch";
            MessageDigest sha=MessageDigest.getInstance("SHA-256");
            try (InputStream in=new FileInputStream(apk)) {
                byte[] buf=new byte[256*1024];
                for (int n;(n=in.read(buf))>=0;) {if(n>0)sha.update(buf,0,n);}
            }
            StringBuilder hex=new StringBuilder(64);
            for(byte v:sha.digest()) hex.append(String.format(Locale.ROOT,"%02x",v&255));
            return IG_SHA256.equals(hex.toString()) ? "EXACT_APK_PASS" : "REFUSED: IG455 APK SHA mismatch";
        } catch (Throwable t) {
            // Never log exception contents or app paths.
            return "REFUSED: APK verification unavailable";
        }
    }

    static Method uiMethod(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> c=Class.forName("X.0Ie0",false,loader);
        Field f=c.getDeclaredField("$t");
        if (f.getType()!=int.class) throw new NoSuchFieldException("selector type");
        Method m=c.getDeclaredMethod("invoke");
        if (m.getReturnType()!=Object.class) throw new NoSuchMethodException("UI result type");
        uiSelector=f;
        return m;
    }
    static Method ufiMethod(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> c=Class.forName("X.0F2B",false,loader);
        Field f=c.getDeclaredField("$t");
        if (f.getType()!=int.class) throw new NoSuchFieldException("selector type");
        Method m=c.getDeclaredMethod("invoke",Object.class);
        if (m.getReturnType()!=Object.class) throw new NoSuchMethodException("UFI result type");
        ufiSelector=f;
        return m;
    }
    static Method mobileConfigMethod(ClassLoader loader) throws ReflectiveOperationException {
        // Verified from the original IG455 APK, classes22.dex:
        // X.0T4C implements BOTH MobileConfigUnsafeContext and X.0mMk.
        // X.01ym.A02 returns X.0mMk at the selector-4 call site.
        // Never call a getter to discover its runtime object or config key.
        return P04ConfigTarget.resolve(loader);
    }
    static Method nullableConfigMethod(ClassLoader loader) throws ReflectiveOperationException {
        // X.0ApG.D3r is an interface method; hook the verified concrete X.02ib.
        Class<?> c=Class.forName("X.02ib",false,loader);
        Method m=c.getDeclaredMethod("D3r",int.class);
        if(m.getReturnType()!=Boolean.class)throw new NoSuchMethodException("D3r result type");
        return m;
    }
    static Method sessionCheckMethod(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> c=Class.forName("X.02xy",false,loader);
        Class<?> session=Class.forName("com.instagram.common.session.UserSession",false,loader);
        Method m=c.getDeclaredMethod("A04",session,String.class);
        if(m.getReturnType()!=boolean.class)throw new NoSuchMethodException("A04 result type");
        return m;
    }
    static void setInputPresence(P04InputPresence probe) {inputPresence=probe;}
    static void installed(int optionalInstalled,String unavailable) {
        // Core state hooks are already installed, so allow A/B even if an
        // optional configuration observer is unavailable on this IG455 build.
        ready=true;
        installStatus="READY: Repost A/B core (2/2), optional " + optionalInstalled + "/3; " +
                "HF05 config-dispatch A/B read-only" +
                (unavailable.isEmpty()?"":"; unavailable: "+unavailable);
    }
    static P04PredicateTrace.Frame enterUi(Object instance) {
        if(!ready || !STATE.recording() || uiSelector==null || instance==null)return null;
        try {if(uiSelector.getInt(instance)!=4)return null;
             P04InputPresence probe=inputPresence;
             int presence=probe==null?P04InputPresence.UNAVAILABLE:probe.classify(instance);
             return P04PredicateTrace.enter(STATE.activeIndex(),presence);}
        catch(Throwable ignored){return null;}
    }
    static void leaveUi(P04PredicateTrace.Frame frame,Object originalResult) {
        P04PredicateTrace.leave(frame,originalResult);
    }
    /** Passive result classification; caller always receives the exact original object. */
    static Object noteUi(Object instance, Object result) {
        if (!STATE.recording() || uiSelector==null || instance==null) return result;
        try {if(uiSelector.getInt(instance)==4)STATE.record(4,result);}
        catch(Throwable ignored) { }
        return result;
    }
    static void noteUfi(Object instance, Object result) {
        if (!STATE.recording() || ufiSelector==null || instance==null) return;
        try {int selector=ufiSelector.getInt(instance);if(selector==11 || selector==12)STATE.record(selector,result);}
        catch (Throwable ignored) { }
    }
    static String begin(int index) {
        if (!ready) return "REFUSED: P04 hooks not ready ("+status()+")";
        STATE.start(index);
        return "P04 recording manual account "+(index==0?"A":"B")+" (no identity read)";
    }
    static String stop() {STATE.stop();return "P04 stopped (read-only)";}
    static String clear() {STATE.clear();P04PredicateTrace.clear();return "P04 capture cleared";}
    static String report() {
        return "P04 EXACT-APK A/B TRACE\nHook: "+status()+"\n"+STATE.render()
            + P04PredicateTrace.report()
            + "Repost eligibility override: NOT INSTALLED; original results preserved.\n";
    }

    static String countVisibleLabels(Activity activity,View excludedPanel,View excludedChip) {
        if (!ready || !STATE.recording()) return "REFUSED: begin A or B and wait for hooks";
        // Transient exact-word comparison only: no UI text or content descriptions are stored.
        View root=activity.getWindow().getDecorView();
        ArrayDeque<View> queue=new ArrayDeque<>(); queue.add(root);
        int visited=0,repost=0,friends=0;
        while (!queue.isEmpty() && visited<1400) {
            View v=queue.removeFirst();
            if(v==excludedPanel || v==excludedChip || v.getVisibility()!=View.VISIBLE)continue;
            visited++;
            String desc=v.getContentDescription()==null?"":v.getContentDescription().toString();
            String word="";
            if (v instanceof TextView) {
                CharSequence t=((TextView)v).getText();
                word=t==null?"":t.toString();
            }
            if (labelIs(desc,"repost","repostar","republicar") ||
                labelIs(word,"repost","repostar","republicar")) repost++;
            if (labelIs(desc,"friends","amigos") || labelIs(word,"friends","amigos")) friends++;
            if(v instanceof ViewGroup) {
                ViewGroup parent=(ViewGroup)v;
                for(int i=0;i<parent.getChildCount() && queue.size()<1400;i++) queue.addLast(parent.getChildAt(i));
            }
        }
        String aggregate="visited="+visited+" repost_labels="+repost+" friends_labels="+friends;
        STATE.uiCheck(aggregate);
        return "UI labels counted: "+aggregate+" (Litho/unrendered elements may be absent)";
    }
    private static boolean labelIs(String text,String... expected) {
        String s=text.trim().toLowerCase(Locale.ROOT);
        for(String candidate:expected)if(candidate.equals(s))return true;
        return false;
    }
}
