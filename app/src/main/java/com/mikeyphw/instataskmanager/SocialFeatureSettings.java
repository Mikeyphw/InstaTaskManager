package com.mikeyphw.instataskmanager;

import android.content.Context;
import android.content.SharedPreferences;

/** Only explicit, default-off requests. Never treats a checkbox as server eligibility. */
final class SocialFeatureSettings {
    static final String[] KEYS = {
            "repost_feed", "repost_reels", "liked_by", "reposted_by", "friendly_bubbles"
    };
    static final String[] LABELS = {
            "Show native Repost · Feed", "Show native Repost · Reels",
            "Show friends who liked", "Show friends who reposted",
            "Show floating friend bubbles"
    };
    private static final String PREFS = "itm_social_p03_requested_only";
    private SocialFeatureSettings() { }
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    static boolean get(Context c, String key) {
        if (indexOf(key) < 0) return false;
        try { return prefs(c).getBoolean(key, false); }
        catch (Throwable ignored) { return false; }
    }
    static boolean put(Context c, String key, boolean requested) {
        if (indexOf(key) < 0) return false;
        try { return prefs(c).edit().putBoolean(key, requested).commit(); }
        catch (Throwable ignored) { return false; }
    }
    static String report(Context c) {
        StringBuilder b = new StringBuilder("SOCIAL REQUESTS (not server permissions)\n");
        for (int i=0;i<KEYS.length;i++) b.append(LABELS[i]).append(": ")
                .append(get(c, KEYS[i]) ? "REQUESTED" : "OFF").append("\n");
        b.append("Only a manually verified, native View can be recovered; ")
         .append("avatar renderers remain unmodified.\n");
        return b.toString();
    }
    private static int indexOf(String key) {
        for (int i=0;i<KEYS.length;i++) if (KEYS[i].equals(key)) return i;
        return -1;
    }
}
