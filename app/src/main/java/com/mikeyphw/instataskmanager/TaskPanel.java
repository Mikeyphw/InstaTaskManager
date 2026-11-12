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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
    private TextView chip, content, captureBanner;
    private Runnable captureTicker;
    private boolean lastP04Ready;
    private Button exportButton;
    private volatile boolean exporting;
    private LinearLayout panel, controlsBar, scrollBody, socialSwitches;
    private final NativeRepostRecovery nativeRecovery = new NativeRepostRecovery();
    private boolean socialExactVerified;
    private String nativeTrial = "No manual native-view attempt yet.";
    private String p04Action = "Capture disabled until P04 hook installation succeeds.";
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
        t.startCaptureTicker();
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

    /** Updates only two TextViews once a second: no extra profiler or APK scans. */
    private void startCaptureTicker() {
        lastP04Ready=P04GateTrace.ready();
        captureTicker=new Runnable() {
            @Override public void run() {
                if (current != TaskPanel.this || activity.isFinishing() || activity.isDestroyed()) return;
                refreshCaptureFeedback();
                if (panel != null && "social".equals(tab) && content != null)
                    renderSocialContent();
                boolean newReady=P04GateTrace.ready();
                if (panel != null && "social".equals(tab) && lastP04Ready != newReady)
                    update(); // enable Start A/B when APK verification finishes
                lastP04Ready=newReady;
                MAIN.postDelayed(this,1000);
            }
        };
        refreshCaptureFeedback();
        MAIN.postDelayed(captureTicker,1000);
    }

    private void refreshCaptureFeedback() {
        int active=P04GateTrace.activeIndex();
        String line=P04GateTrace.feedback();
        if (captureBanner != null) {
            captureBanner.setText(line);
            captureBanner.setTextColor(active >= 0 ? ACCENT : FOREGROUND);
        }
        if (chip != null) {
            boolean trialOn=P04ExactGateTrial.status().startsWith("ARMED");
            chip.setText(trialOn ? "⚡ A" : active == 0 ? "● A" : active == 1 ? "● B" : "TM");
            chip.setTextColor(active >= 0 ? ACCENT : FOREGROUND);
            chip.setContentDescription(active < 0
                    ? "Instagram Task Manager — open performance panel"
                    : "Task Manager recording account " + (active==0 ? "A" : "B") + " — tap to open");
        }
    }

    private void actionToast(String message) {
        String text=message.length()>120 ? message.substring(0,120) : message;
        Toast.makeText(activity.getApplicationContext(),text,Toast.LENGTH_SHORT).show();
    }

    private TextView txt(String s, int size, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s); t.setTextColor(FOREGROUND); t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void openPanel() {
        if (panel != null || activity.isFinishing() || activity.isDestroyed()) return;
        InstaTaskModule.requestP04(activity); // background hash + guarded passive hooks
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
        exportButton=button("Export", this::exportDiagnostics);
        exportButton.setContentDescription("Export Task Manager diagnostics to Downloads");
        header.addView(exportButton,new LinearLayout.LayoutParams(dp(72),dp(40)));
        Button close=button("×",()->closePanel());
        header.addView(close,new LinearLayout.LayoutParams(dp(45),dp(40)));
        panel.addView(header);
        captureBanner=txt("P04 · VERIFYING",12,true);
        captureBanner.setPadding(dp(6),dp(6),dp(6),dp(6));
        captureBanner.setBackground(background(Color.rgb(34,45,58),7,Color.rgb(58,79,94)));
        panel.addView(captureBanner);
        refreshCaptureFeedback();

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
        scrollBody = new LinearLayout(activity);
        scrollBody.setOrientation(LinearLayout.VERTICAL);
        socialSwitches = new LinearLayout(activity);
        socialSwitches.setOrientation(LinearLayout.VERTICAL);
        scrollBody.addView(socialSwitches);
        scrollBody.addView(content);
        scroll.addView(scrollBody);
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
        actionToast("IG455 scan started");
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
                    socialExactVerified = display.contains("EXACT IG455 APK SHA256 + size: MATCH")
                            && display.contains("Matched structures: 9 / 9 | ambiguous: 0 | unavailable: 0");
                    socialReport = display;
                    actionToast(socialExactVerified ? "IG455 scan complete: 9/9 structures" : "IG455 scan stopped or unavailable");
                    if ("social".equals(tab)) update();
                }
            });
        });
    }

    /** A single explicit click saves the latest known readings; no background uploads. */
    private void exportDiagnostics() {
        if (exporting || !sampling || panel == null || worker == null) return;
        final long time = System.currentTimeMillis();
        final ProfilerSampler.Snapshot captured = snapshot;
        final String tabAtExport = tab;
        final String partsAtExport = components;
        final String socialAtExport = (socialScanning
                ? "Scan currently running; no finished results available."
                : socialReport) + "\n\n" + SocialFeatureSettings.report(activity)
                + "\n" + nativeTrial + "\n\nCAPTURE STATUS: " +
                P04GateTrace.feedback() + "\n" + P04GateTrace.report();
        final String overview = overviewText(captured);
        final String threads = threadsText(captured);
        exporting = true;
        if (exportButton != null) {
            exportButton.setText("Saving…");
            exportButton.setEnabled(false);
        }
        actionToast("Saving diagnostics to Downloads…");
        final android.content.Context app = activity.getApplicationContext();
        worker.post(() -> {
            String location = null;
            try {
                String report = ExportReport.build(time, tabAtExport, overview,
                        threads, partsAtExport, socialAtExport);
                location = ExportFile.save(app, time, report);
            } catch (Throwable ignored) {
                // Fail closed; exception messages, file paths and other private data
                // must never be included in UI or shared logs.
            }
            final String success = location;
            MAIN.post(() -> {
                exporting = false;
                if (exportButton != null) {
                    exportButton.setText("Export");
                    exportButton.setEnabled(true);
                }
                Toast.makeText(app, success != null
                        ? "SUCCESS: " + success
                        : "FAILURE: Could not export report to Downloads", Toast.LENGTH_LONG).show();
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
            if(sampling && worker!=null) worker.postDelayed(task[0],SamplingCadence.BASE_INTERVAL_MS);
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
        if (socialSwitches != null) {
            socialSwitches.setVisibility("social".equals(tab) ? View.VISIBLE : View.GONE);
            if ("social".equals(tab)) buildSocialSwitches();
        }
        if(controlsBar != null) {
            controlsBar.removeAllViews();
            if ("social".equals(tab)) {
                Button scan = button("Scan IG455 (read-only)", this::scanSocial);
                scan.setEnabled(!socialScanning);
                controlsBar.addView(scan,new LinearLayout.LayoutParams(0,dp(46),1));
                Button trial = button("Try native Repost", this::tryNativeRepost);
                trial.setEnabled(socialExactVerified && !socialScanning &&
                        (SocialFeatureSettings.get(activity,"repost_feed") ||
                         SocialFeatureSettings.get(activity,"repost_reels")));
                controlsBar.addView(trial,new LinearLayout.LayoutParams(0,dp(46),1));
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
            renderSocialContent();
            return;
        }
        if("controls".equals(tab)) {
            content.setText("CONTROLS\n\n"+
                "Measurement runs only while this panel is open.\n"+
                "Close the panel to stop sampling and release its worker thread.\n\n"+
                "Optional Instagram components: NO VERIFIED SWITCHES YET.\n"+
                "Social features: the Social tab contains requested-only experimental toggles " +
                "and a guarded, manual native Repost View trial.\n"+
                "Disabling services, providers, activities or receivers without an APK-specific " +
                "dependency audit could break login, DM and playback.\n\n"+
                "For reversible device-wide background restrictions, open Instagram's system app settings.\n"+
                "For hiding Reels/Explore/etc, prefer InstaEclipse's existing switches " +
                "(hiding a tab is NOT proof that its network/background work stops).\n\n"+
                "The TM chip can be hidden until the Instagram process restarts.");
            return;
        }
        if("overview".equals(tab)) content.setText(overviewText(snapshot));
        else content.setText(threadsText(snapshot));
    }

    private void renderSocialContent() {
        if (content == null || !"social".equals(tab)) return;
        content.setText("SOCIAL DISCOVERY — IG455\n\n" + socialReport + "\n\n" +
                "P04 ACCOUNT A/B READ-ONLY TRACE\n" + p04Action + "\n" +
                P04GateTrace.report() + "\n" +
                "REQUESTED TOGGLES · EXPERIMENTAL\n" +
                "These switches save your preference; they do not override server permission.\n" +
                "Repost: after an exact scan, Try native Repost only reveals an EXISTING " +
                "hidden clickable native View labelled Repost. Most Instagram views may not qualify.\n" +
                "Friend avatars: no verified safe native presentation gate, so requests are " +
                "PENDING and do NOT yet alter Instagram.\n" +
                "HF08: Repost gate trial changes at most 6 exact local UI Boolean outcomes, only after confirmed A/B split and native Repost data present.\n" +
                nativeTrial + "\n\n" +
                "Disable repost in InstaEclipse must be OFF. No synthetic repost/like data.");
        }

    private static String overviewText(ProfilerSampler.Snapshot s) {
        if(s==null) return "Waiting for first sample…";
        return new StringBuilder()
            .append("INSTAGRAM MAIN PROCESS\n")
            .append("CPU: ").append(ProfilerSampler.format(s.cpuPct)).append("% of one core\n")
            .append("PSS: ").append(ProfilerSampler.mem(s.pssKb)).append("\n")
            .append("RSS: ").append(ProfilerSampler.mem(s.rssKb)).append("\n")
            .append("Java heap used: ").append(ProfilerSampler.mem(s.javaUsedKb)).append("\n")
            .append("Native heap allocated: ").append(ProfilerSampler.mem(s.nativeAllocatedKb)).append("\n")
            .append("Active threads (last scan): ").append(s.threads >= 0 ? s.threads : "pending").append("\n")
            .append("Profiler sampler CPU estimate: ").append(ProfilerSampler.format(s.samplerCpuPct))
            .append("% of one core (sample thread only)\n")
            .append("UID network download: ").append(ProfilerSampler.format(s.inboundKbPerSec)).append(" KiB/s\n")
            .append("UID network upload: ").append(ProfilerSampler.format(s.outboundKbPerSec)).append(" KiB/s\n\n")
            .append("SAME-UID PROCESSES (if exposed):\n").append(s.processes).append("\n")
            .append("The UID network totals may include other Instagram processes.\n")
            .append("PSS/RSS are process totals, not per-feature memory estimates.\n")
            .append("The profiler adds overhead; this sampler-thread estimate excludes UI/hook costs.\n\n")
            .append("CPU/network every 5 s; threads ~20 s; memory ~30 s; processes ~40 s.\n")
            .toString();
    }

    private static String threadsText(ProfilerSampler.Snapshot s) {
        if(s==null) return "Waiting for first sample…";
        StringBuilder b=new StringBuilder();
        b.append("BUSIEST THREADS — current Instagram process\n")
         .append("CPU is per-thread delta since last sample (one core=100%).\n")
         .append("Labels ending with ? are ONLY name heuristics.\n")
         .append("Thread CPU averages the last scan window, ~20 s (first scan is baseline).\n")
         .append("Scan window: ").append(s.threadPeriodMillis > 0 ?
                s.threadPeriodMillis + " ms" : "pending").append("\n\n");
        for(ProfilerSampler.HotThread t:s.topThreads){
            b.append(ProfilerSampler.format(t.cpuPct)).append("%  ")
             .append(t.name).append("\n      ").append(t.hint).append("\n");
        }
        if(s.topThreads.isEmpty()) b.append("No active threads identified, or first sample.\n");
        b.append("\nThread names do NOT identify individual Instagram features with certainty.\n");
        return b.toString();
    }

    private void buildSocialSwitches() {
        if (socialSwitches == null) return;
        socialSwitches.removeAllViews();
        for (int i=0; i<SocialFeatureSettings.KEYS.length;i++) {
            final String key = SocialFeatureSettings.KEYS[i];
            CheckBox box = new CheckBox(activity);
            box.setText(SocialFeatureSettings.LABELS[i] + " (experimental)");
            box.setTextColor(FOREGROUND);
            box.setTextSize(12);
            box.setChecked(SocialFeatureSettings.get(activity, key));
            box.setOnCheckedChangeListener((buttonView,checked) -> {
                if (!SocialFeatureSettings.put(activity, key, checked)) {
                    buttonView.setOnCheckedChangeListener(null);
                    buttonView.setChecked(!checked);
                    Toast.makeText(activity, "Could not save Social setting", Toast.LENGTH_SHORT).show();
                }
                // Turning OFF restores any native visibility manually recovered.
                if (!checked && (key.startsWith("repost_"))) nativeRecovery.restore();
                if (panel != null && "social".equals(tab)) update();
            });
            socialSwitches.addView(box);
        }
        TextView p04Title=txt("P04 · Compare full Instagram account A and B (no IDs)",11,true);
        socialSwitches.addView(p04Title);
        LinearLayout p04row=new LinearLayout(activity);
        Button a=button("Start A",()->p04Start(0));
        Button b=button("Start B",()->p04Start(1));
                Button stop=button("Stop",()->{
                    p04Action=P04GateTrace.stop();
                    actionToast(p04Action);
                    refreshCaptureFeedback();
                    update();
                });
        int recording=P04GateTrace.activeIndex();
        a.setEnabled(P04GateTrace.ready() && recording < 0);
        b.setEnabled(P04GateTrace.ready() && recording < 0);
        stop.setEnabled(recording >= 0);
        if (recording==0) a.setText("● A recording");
        if (recording==1) b.setText("● B recording");
        p04row.addView(a,new LinearLayout.LayoutParams(0,dp(42),1));
        p04row.addView(b,new LinearLayout.LayoutParams(0,dp(42),1));
        p04row.addView(stop,new LinearLayout.LayoutParams(0,dp(42),1));
        socialSwitches.addView(p04row);
        LinearLayout p04row2=new LinearLayout(activity);
        Button inspect=button("Count UI labels",()->{
            p04Action=P04GateTrace.countVisibleLabels(activity,panel,chip);
            actionToast(p04Action);
            update();
        });
        Button clear=button("Clear A/B",()->{
            p04Action=P04GateTrace.clear();
            actionToast(p04Action);
            refreshCaptureFeedback();
            update();
        });
        p04row2.addView(inspect,new LinearLayout.LayoutParams(0,dp(42),1));
        p04row2.addView(clear,new LinearLayout.LayoutParams(0,dp(42),1));
        socialSwitches.addView(p04row2);
        LinearLayout gateRow=new LinearLayout(activity);
        Button gate=button("Try A UI gate",()->{
            p04Action=P04GateTrace.armExactTrial();
            actionToast(p04Action);
            refreshCaptureFeedback();
            update();
        });
        // Keep clickable in A to explain WHY the local experiment is refused.
        gate.setEnabled(P04GateTrace.ready() && recording==0);
        Button gateOff=button("Trial OFF",()->{
            p04Action=P04GateTrace.stopExactTrial();
            actionToast(p04Action);
            refreshCaptureFeedback();
            update();
        });
        gateRow.addView(gate,new LinearLayout.LayoutParams(0,dp(42),1));
        gateRow.addView(gateOff,new LinearLayout.LayoutParams(0,dp(42),1));
        socialSwitches.addView(gateRow);
        TextView trialStatus=txt("HF12 experimental UI check: "+P04ExactGateTrial.status()+
            "; eligibility="+(P04ExactGateTrial.eligibleGate()<0 ? "NOT PROVEN":
            P04ExactGateTrial.LABELS[P04ExactGateTrial.eligibleGate()])+". " +
            "Reason: "+P04ExactGateTrial.reason()+". No fake media or social data.",11,false);
        socialSwitches.addView(trialStatus);
        TextView p04Help=txt("Procedure: with the affected account, Start A, browse the same public " +
                "Reel and feed post, then Stop. Switch accounts, Start B, repeat, Stop, Export. " +
                "Count UI labels is optional; it checks exact words only and saves counts. " +
                "Do not leave the wrong account capture active while switching. " +
                "Only after both accounts produce repeated exact eligibility data with native Repost objects " +
                "present can you return to A, press Start A, then Try A UI gate. HF12 tolerates one false singleton result on B (<=5%); native null results are never modified. " +
                "The trial expires after 30 seconds or 6 changed local results, whichever comes first. " +
                "Stop or Trial OFF disarms it. If Instagram behaves oddly, restart it. " +
                "HF03 records upstream Boolean outcomes in their original call order, only " +
                "inside Repost selector 4. All Instagram results remain unchanged.",11,false);
        socialSwitches.addView(p04Help);
        TextView status = txt("OFF by default. Avatar switches are requested-only; " +
                "no avatar hooks are installed.\nManual Repost trials require a successful " +
                "IG455 scan; no action is performed automatically.",11,false);
        socialSwitches.addView(status);
    }

    private void p04Start(int accountIndex) {
        p04Action=P04GateTrace.begin(accountIndex);
        actionToast(p04Action);
        refreshCaptureFeedback();
        update();
    }

    private void tryNativeRepost() {
        if (!socialExactVerified || socialScanning ||
            (!SocialFeatureSettings.get(activity,"repost_feed") &&
             !SocialFeatureSettings.get(activity,"repost_reels"))) {
            nativeTrial = "REFUSED: Exact IG455 scan + at least one Repost request required.";
        } else {
            // This is a single user-triggered, bounded visibility-only trial.
            // No method invocation or network request occurs.
            nativeTrial = nativeRecovery.tryExisting(decor);
        }
        actionToast(nativeTrial);
        update();
    }

    private void closePanel() {
        stopSampler();
        socialExactVerified = false;
        if(panel!=null){decor.removeView(panel);panel=null;content=null;captureBanner=null;controlsBar=null;exportButton=null;socialSwitches=null;scrollBody=null;}
        refreshCaptureFeedback();
        if(chip!=null) chip.setVisibility(View.VISIBLE);
    }

    private void dispose() {
        if (captureTicker != null) MAIN.removeCallbacks(captureTicker);
        captureTicker=null;
        nativeRecovery.restore();
        closePanel();
        if(chip!=null){decor.removeView(chip);chip=null;}
    }
}
