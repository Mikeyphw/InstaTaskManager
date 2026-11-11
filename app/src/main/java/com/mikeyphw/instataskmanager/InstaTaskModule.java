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
            Log.i("ITM_P00", "INSTALLED main=1 lifecycle_only=1 actions=off");
        } catch (Throwable t) {
            Log.w("ITM_P00", "INSTALL_FAILED type=" + t.getClass().getSimpleName());
            installed.set(false);
        }
    }
}
