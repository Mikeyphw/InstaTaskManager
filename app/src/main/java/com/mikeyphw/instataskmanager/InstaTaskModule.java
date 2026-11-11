package com.mikeyphw.instataskmanager;

import android.app.Activity;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;

/** Modern API 102, Instagram MAIN-process only; never touches Instagram internals. */
public final class InstaTaskModule extends XposedModule {
    private final AtomicBoolean installed = new AtomicBoolean(false);
    private String processName;
    private static volatile InstaTaskModule live;
    private final AtomicBoolean p04Scheduled = new AtomicBoolean(false);

    /** Begins only after the user opens the Task Manager panel. */
    static void requestP04(Activity activity) {
        InstaTaskModule module = live;
        if (module == null || activity == null ||
                !"com.instagram.android".equals(activity.getPackageName()) ||
                !module.p04Scheduled.compareAndSet(false,true)) return;
        final android.content.Context context=activity.getApplicationContext();
        final ClassLoader loader=activity.getClassLoader();
        Thread t=new Thread(() -> module.installP04(context,loader),"ITM-P04-verify");
        t.setDaemon(true);
        t.start();
    }

    private void installP04(android.content.Context context,ClassLoader loader) {
        P04GateTrace.setStatus("VERIFYING exact IG455 APK (no hooks active)");
        String result=P04GateTrace.verifyExactApk(context);
        if (!"EXACT_APK_PASS".equals(result)) {P04GateTrace.setStatus(result);return;}
        try {
            // Exact method-shape preflight for ALL five paths before installing hooks.
            Method ui=P04GateTrace.uiMethod(loader);
            Method ufi=P04GateTrace.ufiMethod(loader);
            Method mobile=P04GateTrace.mobileConfigMethod(loader);
            Method boxed=P04GateTrace.nullableConfigMethod(loader);
            Method session=P04GateTrace.sessionCheckMethod(loader);
            // Exact X.0Ie0.A02 -> X.07XN.A0u data-presence preflight.
            // Only Boolean presence categories, never account/media data.
            P04InputPresence input=P04InputPresence.preflight(loader);
            P04GateTrace.setInputPresence(input);
            hook(mobile).setId("itm-p04-hf03-mobileconfig-boolean")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object original=chain.proceed();
                    try {P04PredicateTrace.note(P04PredicateTrace.MOBILE_CONFIG,original);}
                    catch (Throwable ignored) { } // never affect Instagram calls
                    return original;
                });
            hook(boxed).setId("itm-p04-hf03-config-nullable-boolean")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object original=chain.proceed();
                    try {P04PredicateTrace.note(P04PredicateTrace.ELIGIBILITY,original);}
                    catch (Throwable ignored) { } // never affect Instagram calls
                    return original;
                });
            hook(session).setId("itm-p04-hf03-session-boolean")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object original=chain.proceed();
                    try {P04PredicateTrace.note(P04PredicateTrace.SESSION_CHECK,original);}
                    catch (Throwable ignored) { } // never affect Instagram calls
                    return original;
                });
            hook(ui).setId("itm-p04-repost-ui-sel4")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    P04PredicateTrace.Frame frame=P04GateTrace.enterUi(chain.getThisObject());
                    try {
                        Object resultValue=chain.proceed(); // ORIGINAL exactly once
                        P04GateTrace.leaveUi(frame,resultValue);
                        frame=null;
                        try {return P04GateTrace.noteUi(chain.getThisObject(),resultValue);}
                        catch (Throwable ignored) {return resultValue;}
                    } finally {
                        P04GateTrace.leaveUi(frame,null); // exception path restores ThreadLocal
                    }
                });
            hook(ufi).setId("itm-p04-repost-ufi-sel11-12")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object resultValue=chain.proceed(); // ORIGINAL exactly once
                    try {P04GateTrace.noteUfi(chain.getThisObject(),resultValue);}
                    catch (Throwable ignored) { }
                    return resultValue;
                });
            P04GateTrace.installed();
        } catch (Throwable t) {
            P04GateTrace.setStatus("REFUSED: exact hook shape unavailable ("+
                    t.getClass().getSimpleName()+")");
        }
    }


    public InstaTaskModule() { super(); }

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        Log.i("ITM_P00", "MODULE_LOADED main=" + "com.instagram.android".equals(processName));
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!"com.instagram.android".equals(param.getPackageName()) ||
            !"com.instagram.android".equals(processName) ||
            !installed.compareAndSet(false, true)) return;
        try {
            Method resumed = Activity.class.getDeclaredMethod("onResume");
            hook(resumed).setId("itm-p00-resume")
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    try {
                        Object owner = chain.getThisObject();
                        if (owner instanceof Activity) {
                            Activity activity = (Activity) owner;
                            if ("com.instagram.android".equals(activity.getPackageName()))
                                TaskPanel.onResumed(activity);
                        }
                    } catch (Throwable ignored) {
                        // Never change the behavior of the original Activity lifecycle.
                    }
                    return result;
                });
            for (String name : new String[]{"onPause", "onDestroy"}) {
                Method method = Activity.class.getDeclaredMethod(name);
                hook(method).setId("itm-p00-" + name)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        try { return chain.proceed(); }
                        finally {
                            try {
                                Object owner = chain.getThisObject();
                                if (owner instanceof Activity) TaskPanel.onLeaving((Activity) owner);
                            } catch (Throwable ignored) { }
                        }
                    });
            }
            live=this;
            Log.i("ITM_P00", "INSTALLED main=1 lifecycle_only=1 actions=off");
        } catch (Throwable t) {
            Log.w("ITM_P00", "INSTALL_FAILED type=" + t.getClass().getSimpleName());
            installed.set(false);
        }
    }
}
