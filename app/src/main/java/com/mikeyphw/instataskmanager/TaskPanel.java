package com.mikeyphw.instataskmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A nonmodal, draggable in-app performance panel. Sampling exists ONLY while open. */
final class TaskPanel {
    private static final int BG = Color.rgb(20,26,35);
    private static final int FOREGROUND = Color.rgb(237,244,247);
    private static final int ACCENT = Color.rgb(72,203,191);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static TaskPanel current;
    private static boolean hiddenUntilProcessRestart;

    private final Activity activity;
    private final ViewGroup decor;
    private TextView chip, content;
    private LinearLayout panel, controlsBar;
    private HandlerThread thread;
    private Handler worker;
    private volatile boolean sampling;
    private ProfilerSampler sampler;
    private String tab = "overview";
    private ProfilerSampler.Snapshot snapshot;
    private String components;
    private String socialReport = "Not scanned. Tap Scan IG455 to inspect read-only method shapes.";
    private volatile boolean socialScanning;
    private int panelTopPx;

    private TaskPanel(Activity activity, ViewGroup decor) {
        this.activity = activity;
        this.decor = decor;
    }

    static void onResumed(Activity activity) {
        if (hiddenUntilProcessRestart || activity.isFinishing() || activity.isDestroyed()) return;
        if (!(activity.getWindow().getDecorView() instanceof FrameLayout)) return;
        if (current != null && current.activity == activity) return;
        if (current != null) current.dispose();
        TaskPanel t = new TaskPanel(activity, (ViewGroup) activity.getWindow().getDecorView());
        current = t;
        t.addChip();
    }

    static void onLeaving(Activity activity) {
        if (current != null && current.activity == activity) {
            current.dispose();
            current = null;
        }
    }

    private int dp(float n) { return (int) (n * activity.getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable background(int color, int radiusDp, int borderColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        bg.setStroke(dp(1), borderColor);
        return bg;
    }

    private void addChip() {
        chip = new TextView(activity);
        chip.setText("TM");
        chip.setContentDescription("Instagram Task Manager — open performance panel");
        chip.setTextSize(14);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setTextColor(FOREGROUND);
        chip.setBackground(background(BG, 12, ACCENT));
        chip.setElevation(dp(8));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(48),dp(42),Gravity.TOP | Gravity.END);
        params.topMargin=dp(84); params.rightMargin=dp(10);
        decor.addView(chip,params);
        chip.setOnTouchListener(new View.OnTouchListener() {
            float downY; int startMargin; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY=e.getRawY(); startMargin=((FrameLayout.LayoutParams)chip.getLayoutParams()).topMargin;
                        moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(e.getRawY()-downY)>dp(8)) moved=true;
                        if (moved) {
                            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)chip.getLayoutParams();
                            lp.topMargin=Math.max(dp(12),Math.min(decor.getHeight()-dp(60), startMargin+(int)(e.getRawY()-downY)));
                            chip.setLayoutParams(lp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggle();
                        return true;
                    default:return true;
                }
            }
        });
    }

    private void toggle() { if (panel != null) closePanel(); else openPanel(); }

    private TextView txt(String s, int size, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s); t.setTextColor(FOREGROUND); t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void openPanel() {
        if (panel != null || activity.isFinishing() || activity.isDestroyed()) return;
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(9), dp(10), dp(8));
        panel.setElevation(dp(10));
        panel.setBackground(background(BG, 15, ACCENT));
        DisplayMetrics metrics=activity.getResources().getDisplayMetrics();
        int width=Math.min(dp(330), Math.max(dp(220),metrics.widthPixels - dp(24)));
        int height=Math.min(dp(405), (int)(metrics.heightPixels * .68));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height,Gravity.TOP|Gravity.END);
        panelTopPx=Math.min(dp(140), Math.max(dp(30),metrics.heightPixels-height-dp(15)));
        lp.topMargin=panelTopPx; lp.rightMargin=dp(12);

        LinearLayout header=new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=txt("Instagram Task Manager",15,true);
        LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(0,dp(40),1);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(heading,titleLp);
        Button close=button("×",()->closePanel());
        header.addView(close,new LinearLayout.LayoutParams(dp(45),dp(40)));
        panel.addView(header);

        // Allows repositioning while leaving the surrounding Instagram screen usable.
        header.setOnTouchListener(new View.OnTouchListener(){
            float down; int original;
            @Override public boolean onTouch(View v,MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){down=e.getRawY();original=panelTopPx;return true;}
                if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                    panelTopPx=Math.max(dp(6), Math.min(decor.getHeight()-panel.getHeight()-dp(6), original+(int)(e.getRawY()-down)));
                    FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)panel.getLayoutParams();
                    p.topMargin=panelTopPx;panel.setLayoutParams(p);return true;
                }
                return e.getActionMasked()==MotionEvent.ACTION_UP;
            }
        });
        // Two compact rows keep all five sections legible on narrow phones.
        LinearLayout tabs=new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.VERTICAL);
        for (String[][] group : new String[][][]{
                {{"Overview","overview"},{"Threads","threads"},{"Parts","components"}},
                {{"Social","social"},{"Controls","controls"}}}) {
            LinearLayout row=new LinearLayout(activity);
            row.setGravity(Gravity.CENTER);
            for (String[] t:group) {
                Button b=button(t[0],()->selectTab(t[1]));
                row.addView(b,new LinearLayout.LayoutParams(0,dp(35),1));
            }
            tabs.addView(row);
        }
        panel.addView(tabs);
        ScrollView scroll = new ScrollView(activity);
        content = txt("Collecting measurements…",12,false);
        content.setTypeface(Typeface.MONOSPACE);
        content.setPadding(dp(4),dp(9),dp(4),dp(12));
        scroll.addView(content);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        controlsBar = new LinearLayout(activity);
        controlsBar.setGravity(Gravity.CENTER);
        panel.addView(controlsBar);
        decor.addView(panel,lp);
        chip.setVisibility(View.GONE);
        startSampler();
    }

    private Button button(String name, Runnable action) {
        Button b=new Button(activity);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setText(name);
        b.setOnClickListener(v->action.run());
        return b;
    }

    private void selectTab(String target) {
        tab=target;
        if ("components".equals(target) && components==null) {
            components="Inspecting Android manifest...";
            if (worker != null) worker.post(() -> {
                String report=ComponentCatalog.describe(activity.getApplicationContext());
                MAIN.post(() -> {
                    if (panel != null) {components=report;if("components".equals(tab)) update();}
                });
            });
        }
        update();
    }

    private void scanSocial() {
        if (worker == null || !sampling || socialScanning) return;
        socialScanning = true;
        socialReport = "Checking installed IG455 APK hash and method shapes…\nNo Instagram methods will be called.";
        update();
        worker.post(() -> {
            String result;
            try {
                result = SocialDiscovery.inspect(activity.getApplicationContext(),
                    activity.getClassLoader(), () -> sampling && socialScanning);
            } catch (Throwable ignored) {
                result = "Social discovery unavailable. No hook changes were made.";
            }
            final String display = result;
            MAIN.post(() -> {
                socialScanning = false;
                if (panel != null && sampling) {
                    socialReport = display;
                    if ("social".equals(tab)) update();
                }
            });
        });
    }

    private void startSampler() {
        if (sampling) return;
        sampling=true;
        thread=new HandlerThread("ITM-profiler",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        worker=new Handler(thread.getLooper());
        sampler=new ProfilerSampler(activity.getApplicationContext());
        final Runnable[] task=new Runnable[1];
        task[0]=()->{
            if(!sampling) return;
            try {
                ProfilerSampler.Snapshot s=sampler.capture();
                MAIN.post(()->{ if(sampling && panel!=null) {snapshot=s;if("overview".equals(tab) || "threads".equals(tab)) update();} });
            } catch (Throwable err) {
                MAIN.post(()->{ if(sampling && content!=null) content.setText("Sampling unavailable for this process."); });
            }
            if(sampling && worker!=null) worker.postDelayed(task[0],2500);
        };
        worker.post(task[0]);
    }

    private void stopSampler() {
        socialScanning=false;
        sampling=false;
        if(worker!=null) worker.removeCallbacksAndMessages(null);
        if(thread!=null) thread.quitSafely();
        worker=null; thread=null; sampler=null;
    }

    private void update() {
        if(content==null) return;
        if(controlsBar != null) {
            controlsBar.removeAllViews();
            if ("social".equals(tab)) {
                Button scan = button("Scan IG455 (read-only)", this::scanSocial);
                scan.setEnabled(!socialScanning);
                controlsBar.addView(scan,new LinearLayout.LayoutParams(-1,dp(46)));
            }
            if ("controls".equals(tab)) {
                Button settings=button("Android app settings", () -> {
                    Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:com.instagram.android"));
                    try {activity.startActivity(intent);} catch (Throwable ignored) { }
                });
                controlsBar.addView(settings,new LinearLayout.LayoutParams(0,dp(46),1));
                Button hide=button("Hide TM chip", () -> {
                    hiddenUntilProcessRestart=true;
                    dispose();
                    if(current==this)current=null;
                });
                controlsBar.addView(hide,new LinearLayout.LayoutParams(0,dp(46),1));
            }
        }
        if("components".equals(tab)) { content.setText(components==null?"Inspecting…":components); return; }
        if("social".equals(tab)) {
            content.setText("SOCIAL DISCOVERY — IG455\n\n" + socialReport + "\n\n" +
                "Repost/facepile overrides remain disabled. This scan cannot prove " +
                "server permission or availability of friends' data.\n" +
                "Use dedicated future verification before enabling UI controls.");
            return;
        }
        if("controls".equals(tab)) {
            content.setText("CONTROLS\n\n"+
                "Measurement runs only while this panel is open.\n"+
                "Close the panel to stop sampling and release its worker thread.\n\n"+
                "Optional Instagram components: NO VERIFIED SWITCHES YET.\n"+
                "Social features: use the Social tab for a read-only Instagram 455 audit.\n"+
                "Disabling services, providers, activities or receivers without an APK-specific " +
                "dependency audit could break login, DM and playback.\n\n"+
                "For reversible device-wide background restrictions, open Instagram's system app settings.\n"+
                "For hiding Reels/Explore/etc, prefer InstaEclipse's existing switches " +
                "(hiding a tab is NOT proof that its network/background work stops).\n\n"+
                "The TM chip can be hidden until the Instagram process restarts.");
            return;
        }
        ProfilerSampler.Snapshot s=snapshot;
        if(s==null){content.setText("Waiting for first sample…");return;}
        StringBuilder b=new StringBuilder();
        if("overview".equals(tab)){
            b.append("INSTAGRAM MAIN PROCESS\n")
             .append("CPU: ").append(ProfilerSampler.format(s.cpuPct)).append("% of one core\n")
             .append("PSS: ").append(ProfilerSampler.mem(s.pssKb)).append("\n")
             .append("RSS: ").append(ProfilerSampler.mem(s.rssKb)).append("\n")
             .append("Java heap used: ").append(ProfilerSampler.mem(s.javaUsedKb)).append("\n")
             .append("Native heap allocated: ").append(ProfilerSampler.mem(s.nativeAllocatedKb)).append("\n")
             .append("Active threads: ").append(s.threads).append("\n")
             .append("UID network download: ").append(ProfilerSampler.format(s.inboundKbPerSec)).append(" KiB/s\n")
             .append("UID network upload: ").append(ProfilerSampler.format(s.outboundKbPerSec)).append(" KiB/s\n\n")
             .append("SAME-UID PROCESSES (if exposed):\n").append(s.processes).append("\n")
             .append("The UID network totals may include other Instagram processes.\n")
             .append("PSS/RSS are process totals, not per-feature memory estimates.\n")
             .append("The profiler itself adds small overhead.\n\n")
             .append("Live sampling: 2.5 s; only with panel open.\n");
        } else {
            b.append("BUSIEST THREADS — current Instagram process\n")
             .append("CPU is per-thread delta since last sample (one core=100%).\n")
             .append("Labels ending with ? are ONLY name heuristics.\n\n");
            for(ProfilerSampler.HotThread t:s.topThreads){
                b.append(ProfilerSampler.format(t.cpuPct)).append("%  ")
                 .append(t.name).append("\n      ").append(t.hint).append("\n");
            }
            if(s.topThreads.isEmpty()) b.append("No active threads identified, or first sample.\n");
            b.append("\nThread names do NOT identify individual Instagram features with certainty.\n");
        }
        content.setText(b.toString());
    }

    private void closePanel() {
        stopSampler();
        if(panel!=null){decor.removeView(panel);panel=null;content=null;controlsBar=null;}
        if(chip!=null) chip.setVisibility(View.VISIBLE);
    }

    private void dispose() {
        closePanel();
        if(chip!=null){decor.removeView(chip);chip=null;}
    }
}
