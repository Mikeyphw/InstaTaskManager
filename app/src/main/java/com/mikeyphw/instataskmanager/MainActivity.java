package com.mikeyphw.instataskmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** No module settings or control privileges; intentionally no service/receiver. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40,48,40,30);
        layout.setBackgroundColor(Color.rgb(17,22,32));
        TextView text = new TextView(this);
        text.setTextSize(17);
        text.setTextColor(Color.WHITE);
        text.setText("Instagram Task Manager — P00 + Social P01\n\n"+
            "Enable this module for Instagram only in Vector (API 102). " +
            "Force-stop and reopen Instagram. Tap the small 'TM' chip to view live metrics.\n\n" +
            "This release measures process/thread CPU, memory and UID network traffic. " +
            "Its Social tab checks IG455 repost/facepile method shapes after exact APK verification. " +
            "No force-enable or spoofing is active until live safety gates are confirmed. " +
            "It cannot reliably attribute PSS to Reels, DMs, or other in-process features.\n\n" +
            "Instagram component disabling is intentionally locked until exact APK-specific dependencies are verified. " +
            "Use Android's battery restrictions or InstaEclipse's own UI switches separately.\n\n" +
            "Profiling stops when the panel closes. No telemetry is uploaded.");
        layout.addView(text);
        Button launch = new Button(this);
        launch.setText("Open Instagram");
        launch.setOnClickListener(v -> {
            Intent i = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
            if (i != null) startActivity(i);
        });
        layout.addView(launch);
        setContentView(layout);
    }
}
