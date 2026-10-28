package com.elan.ghosttouchguard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView info;
    private final Handler handler = new Handler();

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            info.setText("Touches seen: " + LogStore.count + "   Bursts: " + LogStore.bursts + "\n\n" + LogStore.snapshot());
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 64, 32, 32);

        Button perm = new Button(this);
        perm.setText("1. Allow overlay permission");
        perm.setOnClickListener(v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));

        Button start = new Button(this);
        start.setText("2. Start watching");
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow the overlay permission first", Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        });

        Button stop = new Button(this);
        stop.setText("3. Stop watching");
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));

        info = new TextView(this);
        info.setTextSize(14);
        ScrollView sv = new ScrollView(this);
        sv.addView(info);

        root.addView(perm);
        root.addView(start);
        root.addView(stop);
        root.addView(sv);
        setContentView(root);
    }

    @Override
    protected void onResume() { super.onResume(); handler.post(refresh); }

    @Override
    protected void onPause() { super.onPause(); handler.removeCallbacks(refresh); }
}
