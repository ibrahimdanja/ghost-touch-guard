package com.elan.ghosttouchguard;

import java.util.ArrayList;

public class LogStore {
    private static final ArrayList<String> lines = new ArrayList<>();
    public static int count = 0;

    public static synchronized void add(String s) {
        lines.add(s);
        if (lines.size() > 30) lines.remove(0);
    }

    public static synchronized String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (int i = lines.size() - 1; i >= 0; i--) sb.append(lines.get(i)).append("\n");
        return sb.toString();
    }
}
