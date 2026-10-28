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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OverlayService extends Service {
    private WindowManager wm;
    private View dot;
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (dot != null) return START_STICKY;

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("g", "Ghost Touch Guard", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "g");
        } else {
            nb = new Notification.Builder(this);
        }
        Notification n = nb.setContentTitle("Ghost Touch Guard")
                .setContentText("Watching touches")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .build();
        startForeground(1, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        dot = new View(this);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                1, 1, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;

        dot.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                LogStore.count++;
                LogStore.add(fmt.format(new Date()) + "  x=" + (int) e.getRawX() + " y=" + (int) e.getRawY());
            }
            return false;
        });

        wm.addView(dot, p);
        LogStore.add("watching started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (dot != null && wm != null) wm.removeView(dot);
        dot = null;
        LogStore.add("watching stopped");
        super.onDestroy();
    }
}
