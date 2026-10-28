package com.elan.ghosttouchguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class ZoneService extends Service {
    private static final int RADIUS = 70;
    private static final int MERGE = 70;
    private static final int MIN_HITS = 3;

    private WindowManager wm;
    private final ArrayList<View> views = new ArrayList<>();

    @Override
    public IBinder onBind(Intent i) { return null; }

    private ArrayList<float[]> zones() {
        ArrayList<float[]> clusters = new ArrayList<>();
        File f = new File(getExternalFilesDir(null), "ghostmap2.csv");
        if (!f.exists()) return clusters;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length < 9) continue;
                float x = Float.parseFloat(p[7]);
                float y = Float.parseFloat(p[8]);
                float[] hit = null;
                for (float[] c : clusters) {
                    float dx = c[0] - x;
                    float dy = c[1] - y;
                    if (dx * dx + dy * dy <= MERGE * MERGE) { hit = c; break; }
                }
                if (hit == null) {
                    clusters.add(new float[]{x, y, 1});
                } else {
                    hit[0] = (hit[0] * hit[2] + x) / (hit[2] + 1);
                    hit[1] = (hit[1] * hit[2] + y) / (hit[2] + 1);
                    hit[2] += 1;
                }
            }
        } catch (Exception ignored) {}
        ArrayList<float[]> out = new ArrayList<>();
        for (float[] c : clusters) if (c[2] >= MIN_HITS) out.add(c);
        return out;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (wm != null) return START_STICKY;
        ArrayList<float[]> z = zones();

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("g", "Ghost Touch Guard", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "g");
        } else {
            nb = new Notification.Builder(this);
        }
        Notification n = nb.setContentTitle("Ghost Touch Guard")
                .setContentText("Blocking " + z.size() + " spots")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();
        startForeground(3, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        boolean show = getSharedPreferences("cfg", MODE_PRIVATE).getBoolean("show", false);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        for (float[] c : z) {
            View v = new View(this);
            if (show) v.setBackgroundColor(0x44FF0000);
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent e) { return true; }
            });
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    2 * RADIUS, 2 * RADIUS, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.START;
            p.x = (int) c[0] - RADIUS;
            p.y = (int) c[1] - RADIUS;
            wm.addView(v, p);
            views.add(v);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (wm != null) {
            for (View v : views) {
                try { wm.removeView(v); } catch (Exception ignored) {}
            }
        }
        views.clear();
        wm = null;
        super.onDestroy();
    }
}
