package com.elan.ghosttouchguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class BubbleService extends Service {
    private WindowManager wm;
    private TextView bubble;
    private View blocker;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams blockerParams;
    private final Handler handler = new Handler();
    private boolean locked = false;
    private final Runnable autoRelease = new Runnable() {
        @Override
        public void run() { unlock(); }
    };

    @Override
    public IBinder onBind(Intent i) { return null; }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void paint() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(locked ? 0xDDD32F2F : 0xDD1976D2);
        bubble.setBackground(bg);
        bubble.setText(locked ? "HOLD\nto open" : "LOCK");
    }

    private void lock() {
        if (locked) return;
        locked = true;
        blockerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        wm.updateViewLayout(blocker, blockerParams);
        paint();
        int secs = getSharedPreferences("cfg", MODE_PRIVATE).getInt("secs", 60);
        handler.removeCallbacks(autoRelease);
        handler.postDelayed(autoRelease, secs * 1000L);
    }

    private void unlock() {
        if (!locked) return;
        locked = false;
        handler.removeCallbacks(autoRelease);
        blockerParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        wm.updateViewLayout(blocker, blockerParams);
        paint();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (bubble != null) return START_STICKY;

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("g", "Ghost Touch Guard", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "g");
        } else {
            nb = new Notification.Builder(this);
        }
        Notification n = nb.setContentTitle("Ghost Touch Guard")
                .setContentText("Lock bubble is active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();
        startForeground(2, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        blocker = new View(this);
        blocker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) { return true; }
        });
        blockerParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        wm.addView(blocker, blockerParams);

        float d = getResources().getDisplayMetrics().density;
        int size = (int) (60 * d);
        bubble = new TextView(this);
        bubble.setGravity(Gravity.CENTER);
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(11);
        paint();
        bubbleParams = new WindowManager.LayoutParams(
                size, size, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 0;
        bubbleParams.y = (int) (200 * d);

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        touchX = e.getRawX();
                        touchY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - touchX;
                        float dy = e.getRawY() - touchY;
                        if (Math.abs(dx) > 15 || Math.abs(dy) > 15) moved = true;
                        if (moved) {
                            bubbleParams.x = startX + (int) dx;
                            bubbleParams.y = startY + (int) dy;
                            wm.updateViewLayout(bubble, bubbleParams);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            if (!locked) {
                                lock();
                            } else if (e.getEventTime() - e.getDownTime() >= 800) {
                                unlock();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
        wm.addView(bubble, bubbleParams);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(autoRelease);
        if (wm != null) {
            if (bubble != null) wm.removeView(bubble);
            if (blocker != null) wm.removeView(blocker);
        }
        bubble = null;
        blocker = null;
        super.onDestroy();
    }
}
