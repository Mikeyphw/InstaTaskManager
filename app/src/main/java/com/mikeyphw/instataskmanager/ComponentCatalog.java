package com.mikeyphw.instataskmanager;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Manifest inventory ONLY. No component disable APIs, PM mutations or privileged calls. */
final class ComponentCatalog {
    private ComponentCatalog() { }

    static String describe(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("Declared components are NOT separate memory allocations.\n")
           .append("CPU/PSS per component cannot be inferred from Android's manifest.\n\n");
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkg = pm.getPackageInfo("com.instagram.android",
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES |
                    PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS |
                    PackageManager.GET_DISABLED_COMPONENTS);
            add(out, pm, "Services", pkg.services);
            add(out, pm, "Receivers", pkg.receivers);
            add(out, pm, "Providers", pkg.providers);
            add(out, pm, "Activities", pkg.activities);
        } catch (Throwable failure) {
            out.append("Component inventory unavailable: Android/package restrictions.\n");
        }
        out.append("\nToggling arbitrary receivers, providers or services can break ")
           .append("Instagram login, messages, media playback and push notifications.\n")
           .append("No components will be disabled without a verified version-specific allowlist.");
        return out.toString();
    }

    private static void add(StringBuilder out, PackageManager pm, String category, ComponentInfo[] items) {
        if (items == null) { out.append(category).append(": not exposed\n"); return; }
        List<ComponentInfo> list = new ArrayList<>(Arrays.asList(items));
        list.sort(Comparator.comparing(i -> i.name == null ? "" : i.name));
        int disabled = 0;
        List<String> rows = new ArrayList<>();
        for (ComponentInfo item : list) {
            int flag;
            try { flag = pm.getComponentEnabledSetting(new ComponentName("com.instagram.android", item.name)); }
            catch (Throwable denied) { flag = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT; }
            boolean isDisabled = flag == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    flag == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER || !item.enabled;
            if (isDisabled) disabled++;
            if (rows.size() < 25) rows.add((isDisabled ? "OFF  " : "     ") + ProfilerSampler.sanitize(item.name));
        }
        out.append(category).append(": ").append(items.length)
           .append(" declared, ").append(disabled).append(" reported disabled\n");
        for (String row : rows) out.append("  ").append(row).append('\n');
        if (list.size() > rows.size()) out.append("  … +").append(list.size()-rows.size()).append(" more\n");
        out.append('\n');
    }
}
