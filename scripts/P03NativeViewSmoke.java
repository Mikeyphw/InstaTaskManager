package com.mikeyphw.instataskmanager;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Test-only native-view simulations; no Android SDK required. */
public final class P03NativeViewSmoke {
    private static int passed;
    static void eq(boolean actual,boolean expected,String name) {
        if (actual!=expected) throw new AssertionError(name); passed++;
    }
    public static void main(String[] args) {
        eq(NativeRepostRecovery.exactLabel("Repost"),true,"en");
        eq(NativeRepostRecovery.exactLabel("  Repostar "),true,"pt");
        eq(NativeRepostRecovery.exactLabel("Republicar"),true,"pt alt");
        eq(NativeRepostRecovery.exactLabel("Repost to Story"),false,"not exact");
        eq(NativeRepostRecovery.exactLabel("LikED BY Alice"),false,"no avatar");
        eq(NativeRepostRecovery.exactLabel(null),false,"null");
        ViewGroup root=new ViewGroup();
        View hidden=new View();hidden.setVisibility(View.GONE);
        hidden.setContentDescription("Repost");hidden.setHasOnClickListeners(true);
        root.addView(hidden);
        View noListener=new View();noListener.setVisibility(View.GONE);
        noListener.setContentDescription("Repost");root.addView(noListener);
        TextView rep=new TextView();rep.setText("republicar");rep.setVisibility(View.INVISIBLE);
        rep.setHasOnClickListeners(true);root.addView(rep);
        View unlabelled=new View();unlabelled.setVisibility(View.GONE);
        unlabelled.setHasOnClickListeners(true);root.addView(unlabelled);
        NativeRepostRecovery tracker=new NativeRepostRecovery();
        String status=tracker.tryExisting(root);
        eq(hidden.getVisibility()==View.VISIBLE,true,"native button recovered");
        eq(noListener.getVisibility()==View.GONE,true,"nonclickable remains hidden");
        eq(rep.getVisibility()==View.VISIBLE,true,"native text button recovered");
        eq(unlabelled.getVisibility()==View.GONE,true,"unlabelled remains hidden");
        eq(status.contains("2 newly shown"),true,"bounded aggregate report");
        tracker.restore();
        eq(hidden.getVisibility()==View.GONE,true,"native visibility restored");
        eq(rep.getVisibility()==View.INVISIBLE,true,"native text visibility restored");
        eq(noListener.getVisibility()==View.GONE,true,"never changed stays hidden");
        View recycled=new View();recycled.setVisibility(View.GONE);
        recycled.setContentDescription("Repost");recycled.setHasOnClickListeners(true);
        root.addView(recycled);
        tracker.tryExisting(root);
        recycled.setContentDescription("Send message");
        tracker.restore();
        eq(recycled.getVisibility()==View.VISIBLE,true,"recycled unrelated view not hidden");
        System.out.println("ITM_P03_NATIVE_VIEW_SMOKE_PASS " + passed + "/" + passed);
    }
}
