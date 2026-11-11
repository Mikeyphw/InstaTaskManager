package com.mikeyphw.instataskmanager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manual, reversible view-only experiment for IG455. Does not create buttons,
 * attach listeners, dispatch repost actions, spoof Media data, or use network.
 * This can only work if Instagram has ALREADY created a labelled clickable View
 * and hidden that exact View. Litho-only/absent buttons will not be recovered.
 */
final class NativeRepostRecovery {
    static final int MAX_VISITED = 1200;
    private static final String[] LABELS = {"repost", "repostar", "republicar"};
    private final List<OriginalState> modified = new ArrayList<>();

    private static final class OriginalState {
        final WeakReference<View> view;
        final int visibility;
        OriginalState(View v) { view = new WeakReference<>(v); visibility = v.getVisibility(); }
    }

    static boolean exactLabel(String s) {
        if (s == null) return false;
        String normalized = s.trim().toLowerCase(Locale.ROOT);
        for (String name : LABELS) if (name.equals(normalized)) return true;
        return false;
    }

    /** Must be called on main thread, only after verified IG455 scan + consent. */
    String tryExisting(View root) {
        if (root == null) return "REFUSED: No active Instagram view.";
        ArrayDeque<View> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0, labeled = 0, recovered = 0;
        while (!queue.isEmpty() && visited < MAX_VISITED) {
            View v = queue.removeFirst();
            visited++;
            CharSequence desc = v.getContentDescription();
            boolean matched = desc != null && exactLabel(desc.toString());
            if (!matched && v instanceof TextView) {
                CharSequence label = ((TextView)v).getText();
                matched = label != null && exactLabel(label.toString());
            }
            if (matched) {
                labeled++;
                // A native Instagram listener must ALREADY exist; nothing is
                // made clickable here, and the original click path is unchanged.
                if (v.getVisibility() != View.VISIBLE && v.hasOnClickListeners()) {
                    modified.add(new OriginalState(v));
                    v.setVisibility(View.VISIBLE);
                    recovered++;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup group = (ViewGroup)v;
                for (int i=0;i<group.getChildCount() && queue.size()<MAX_VISITED;i++)
                    queue.addLast(group.getChildAt(i));
            }
        }
        return "Native View trial: inspected up to " + visited + " views, "
                + labeled + " labelled native Repost views, " + recovered + " newly shown. "
                + "0 does not prove the feature is unavailable. No native actions were invoked.";
    }

    void restore() {
        for (OriginalState state : modified) {
            View view = state.view.get();
            if (view != null) try {
                CharSequence description = view.getContentDescription();
                boolean stillRepost = description != null && exactLabel(description.toString());
                if (!stillRepost && view instanceof TextView) {
                    CharSequence t = ((TextView)view).getText();
                    stillRepost = t != null && exactLabel(t.toString());
                }
                if (stillRepost) view.setVisibility(state.visibility);
            } catch (Throwable ignored) { }
        }
        modified.clear();
    }
}
