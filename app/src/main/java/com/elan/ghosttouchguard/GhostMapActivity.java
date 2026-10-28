package com.elan.ghosttouchguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GhostMapActivity extends Activity {
    private HeatView heat;
    private TextView summary;
    private Button rec;
    private File csv;
    private int total = 0;
    private int maxFingers = 0;
    private String lastTime = "-";
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        csv = new File(getExternalFilesDir(null), "ghostmap2.csv");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams w = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

        rec = new Button(this);
        rec.setText("Record");
        Button share = new Button(this);
        share.setText("Share CSV");
        Button clear = new Button(this);
        clear.setText("Clear (hold)");
        bar.addView(rec, w);
        bar.addView(share, w);
        bar.addView(clear, w);

        summary = new TextView(this);
        summary.setPadding(24, 8, 24, 8);
        summary.setTextSize(13);

        heat = new HeatView(this);
        load();

        root.addView(bar);
        root.addView(summary);
        root.addView(heat, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        rec.setOnClickListener(v -> {
            heat.recording = !heat.recording;
            rec.setText(heat.recording ? "Stop" : "Record");
            updateSummary();
        });

        share.setOnClickListener(v -> shareCsv());

        clear.setOnLongClickListener(v -> {
            heat.reset();
            if (csv.exists()) csv.delete();
            total = 0;
            maxFingers = 0;
            lastTime = "-";
            updateSummary();
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
            return true;
        });

        heat.setListener((x, y, nx, ny, fingers) -> {
            total++;
            if (fingers > maxFingers) maxFingers = fingers;
            lastTime = fmt.format(new Date());
            int[] loc = new int[2]; heat.getLocationOnScreen(loc); append(x, y, nx, ny, fingers, x + loc[0], y + loc[1]);
            updateSummary();
        });

        updateSummary();
    }

    private void load() {
        if (!csv.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] p = line.split(",");
                if (p.length < 7) continue;
                heat.addNormalized(Float.parseFloat(p[4]), Float.parseFloat(p[5]));
                total++;
                int f = Integer.parseInt(p[6]);
                if (f > maxFingers) maxFingers = f;
                lastTime = p[1];
            }
        } catch (Exception ignored) {}
    }

    private void append(int x, int y, float nx, float ny, int fingers, int sx, int sy) {
        try {
            boolean fresh = !csv.exists();
            FileWriter fw = new FileWriter(csv, true);
            if (fresh) fw.write("epoch_ms,time,x,y,nx,ny,fingers,sx,sy\n");
            fw.write(System.currentTimeMillis() + "," + lastTime + "," + x + "," + y + ","
                    + String.format(Locale.US, "%.3f", nx) + ","
                    + String.format(Locale.US, "%.3f", ny) + "," + fingers + "," + sx + "," + sy + "\n");
            fw.close();
        } catch (Exception ignored) {}
    }

    private void shareCsv() {
        if (!csv.exists()) {
            Toast.makeText(this, "Nothing recorded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
            }
            String s = sb.toString();
            if (s.length() > 200000) {
                s = s.substring(s.length() - 200000);
                s = s.substring(s.indexOf('\n') + 1);
            }
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, s);
            startActivity(Intent.createChooser(i, "Share ghost touch data"));
        } catch (Exception e) {
            Toast.makeText(this, "Could not read the file", Toast.LENGTH_SHORT).show();
        }
    }

    private String name(int i) {
        float nx = ((i % HeatView.COLS) + 0.5f) / HeatView.COLS;
        float ny = ((i / HeatView.COLS) + 0.5f) / HeatView.ROWS;
        String x = nx < 0.34f ? "left" : nx > 0.66f ? "right" : "middle";
        String y = ny < 0.34f ? "top" : ny > 0.66f ? "bottom" : "center";
        return y + " " + x;
    }

    private void updateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(heat.recording ? "RECORDING" : "Paused")
                .append("  |  touches: ").append(total)
                .append("  |  most fingers at once: ").append(maxFingers)
                .append("\nlast: ").append(lastTime)
                .append("\nhottest: ");
        boolean[] used = new boolean[heat.cells.length];
        for (int k = 0; k < 3; k++) {
            int best = -1;
            for (int i = 0; i < heat.cells.length; i++) {
                if (!used[i] && heat.cells[i] > 0 && (best == -1 || heat.cells[i] > heat.cells[best])) best = i;
            }
            if (best == -1) break;
            used[best] = true;
            sb.append(name(best)).append(" (").append(heat.cells[best]).append(")  ");
        }
        summary.setText(sb.toString());
    }
}
