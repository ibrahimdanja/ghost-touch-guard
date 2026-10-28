package com.elan.ghosttouchguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (!Settings.canDrawOverlays(c)) return;
        SharedPreferences sp = c.getSharedPreferences("cfg", Context.MODE_PRIVATE);
        if (!sp.getBoolean("autostart", false)) return;
        Intent z = new Intent(c, ZoneService.class);
        Intent b = new Intent(c, BubbleService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            c.startForegroundService(z);
            c.startForegroundService(b);
        } else {
            c.startService(z);
            c.startService(b);
        }
    }
}
