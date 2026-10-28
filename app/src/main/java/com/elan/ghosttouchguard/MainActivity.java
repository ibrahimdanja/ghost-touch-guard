package com.elan.ghosttouchguard;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Ghost Touch Guard\nBuild pipeline works.");
        tv.setTextSize(22);
        tv.setPadding(48, 96, 48, 48);
        setContentView(tv);
    }
}
