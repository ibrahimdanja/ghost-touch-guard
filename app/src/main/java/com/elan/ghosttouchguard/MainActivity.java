package com.elan.ghosttouchguard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView info;
    private EditText secs;
    private final Handler handler = new Handler();

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            info.setText("Touches seen: " + LogStore.count + "   Bursts: " + LogStore.bursts + "\n\n" + LogStore.snapshot());
            handler.postDelayed(this, 500);
        }
    };

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    private void startSvc(Class<?> c) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow the overlay permission first", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(this, c);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void saveSecs() {
        int s = 60;
        try { s = Integer.parseInt(secs.getText().toString().trim()); } catch (Exception ignored) {}
        if (s < 5) s = 5;
        if (s > 3600) s = 3600;
        getSharedPreferences("cfg", MODE_PRIVATE).edit().putInt("secs", s).apply();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 64, 32, 32);

        root.addView(btn("1. Allow overlay permission", v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())))));

        TextView label = new TextView(this);
        label.setText("Auto-release lock after (seconds):");
        root.addView(label);
        secs = new EditText(this);
        secs.setInputType(InputType.TYPE_CLASS_NUMBER);
        secs.setText(String.valueOf(getSharedPreferences("cfg", MODE_PRIVATE).getInt("secs", 60)));
        root.addView(secs);

        root.addView(btn("2. Start lock bubble", v -> { saveSecs(); startSvc(BubbleService.class); }));
        root.addView(btn("Stop lock bubble", v -> stopService(new Intent(this, BubbleService.class))));
        root.addView(btn("Start touch watcher", v -> startSvc(OverlayService.class)));
        root.addView(btn("Stop touch watcher", v -> stopService(new Intent(this, OverlayService.class))));

        info = new TextView(this);
        info.setTextSize(14);
        ScrollView sv = new ScrollView(this);
        sv.addView(info);
        root.addView(sv);
        setContentView(root);
    }

    @Override
    protected void onResume() { super.onResume(); handler.post(refresh); }

    @Override
    protected void onPause() { super.onPause(); saveSecs(); handler.removeCallbacks(refresh); }
}
