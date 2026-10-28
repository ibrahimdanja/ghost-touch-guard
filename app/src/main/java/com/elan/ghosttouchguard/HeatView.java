package com.elan.ghosttouchguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class HeatView extends View {
    public static final int COLS = 12;
    public static final int ROWS = 20;

    public interface Listener {
        void onTouchLogged(int x, int y, float nx, float ny, int fingers);
    }

    public final int[] cells = new int[COLS * ROWS];
    public boolean recording = false;
    private Listener listener;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HeatView(Context c) {
        super(c);
        line.setColor(0x33AAAAAA);
        line.setStrokeWidth(1f);
        text.setColor(0xDDFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(11f * c.getResources().getDisplayMetrics().scaledDensity);
    }

    public void setListener(Listener l) { listener = l; }

    public void addNormalized(float nx, float ny) {
        int col = Math.min(COLS - 1, Math.max(0, (int) (nx * COLS)));
        int row = Math.min(ROWS - 1, Math.max(0, (int) (ny * ROWS)));
        cells[row * COLS + col]++;
    }

    public void reset() {
        for (int i = 0; i < cells.length; i++) cells[i] = 0;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (recording && (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN)) {
            int i = e.getActionIndex();
            float x = e.getX(i);
            float y = e.getY(i);
            float nx = x / getWidth();
            float ny = y / getHeight();
            addNormalized(nx, ny);
            invalidate();
            if (listener != null) listener.onTouchLogged((int) x, (int) y, nx, ny, e.getPointerCount());
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF101018);
        int max = 1;
        for (int c : cells) if (c > max) max = c;
        float cw = getWidth() / (float) COLS;
        float ch = getHeight() / (float) ROWS;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int n = cells[r * COLS + c];
                if (n > 0) {
                    int alpha = 70 + (int) (185f * n / max);
                    fill.setColor(Color.argb(alpha, 255, 80, 60));
                    canvas.drawRect(c * cw, r * ch, (c + 1) * cw, (r + 1) * ch, fill);
                    canvas.drawText(String.valueOf(n), c * cw + cw / 2f, r * ch + ch / 2f + text.getTextSize() / 3f, text);
                }
            }
        }
        for (int c = 0; c <= COLS; c++) canvas.drawLine(c * cw, 0, c * cw, getHeight(), line);
        for (int r = 0; r <= ROWS; r++) canvas.drawLine(0, r * ch, getWidth(), r * ch, line);
    }
}
