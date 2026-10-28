package com.elan.ghosttouchguard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

public class LogStore {
    private static final ArrayList<String> lines = new ArrayList<>();
    private static final ArrayList<Long> recent = new ArrayList<>();
    public static int count = 0;
    public static int bursts = 0;
    public static File csv;

    public static synchronized void add(String s) {
        lines.add(s);
        if (lines.size() > 40) lines.remove(0);
    }

    public static synchronized void touch(String time, int x, int y) {
        long now = System.currentTimeMillis();
        recent.add(now);
        while (recent.size() > 0 && now - recent.get(0) > 500) recent.remove(0);
        boolean burst = recent.size() >= 3;
        count++;
        if (burst) bursts++;
        add(time + "  x=" + x + " y=" + y + (burst ? "  BURST" : ""));
        write(now + "," + time + "," + x + "," + y + "," + (burst ? 1 : 0));
    }

    private static void write(String line) {
        if (csv == null) return;
        try {
            boolean fresh = !csv.exists();
            FileWriter w = new FileWriter(csv, true);
            if (fresh) w.write("epoch_ms,time,x,y,burst\n");
            w.write(line + "\n");
            w.close();
        } catch (Exception ignored) {}
    }

    public static synchronized String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (int i = lines.size() - 1; i >= 0; i--) sb.append(lines.get(i)).append("\n");
        return sb.toString();
    }
}
