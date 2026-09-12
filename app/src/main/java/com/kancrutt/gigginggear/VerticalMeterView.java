package com.kancrutt.gigginggear;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class VerticalMeterView extends View {

    private static final int SEGMENTS = 14;
    private static final float MAX_LEVEL = 1.0f;

    private float level = 0f;
    private final Paint trackPaint;
    private final Paint fillPaint;
    private final Paint segGapPaint;

    public VerticalMeterView(Context context) {
        this(context, null);
    }

    public VerticalMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(Color.parseColor("#1E2024"));
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#00E676"));
        segGapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        segGapPaint.setColor(Color.parseColor("#0C0D0F"));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setLevel(float value) {
        float v = Math.max(0f, Math.min(MAX_LEVEL, value));
        if (v != level) {
            level = v;
            postInvalidateOnAnimation();
        }
    }

    public void setProgress(int progress) {
        setLevel(progress / 100f);
    }

    public void setProgressTintList(ColorStateList tint) {
        // Segment colors are intrinsic; tint is accepted for API compatibility.
    }

    public void reset() {
        level = 0f;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float barLeft = w * 0.3f;
        float barRight = w * 0.7f;
        float segGap = Math.max(1f, h * 0.004f);
        float segH = (h - segGap * (SEGMENTS - 1)) / SEGMENTS;
        RectF seg = new RectF(barLeft, 0, barRight, 0);

        // idx 0 = top row, idx SEGMENTS-1 = bottom row. Meters rise from the
        // bottom: the `lit` lowest rows light up, red at the top.
        int lit = (int) Math.ceil(level * SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            int idx = SEGMENTS - 1 - i;
            float top = idx * (segH + segGap);
            seg.top = top;
            seg.bottom = top + segH;
            int height = SEGMENTS - 1 - idx; // 0 = bottom row
            if (height < lit) {
                fillPaint.setColor(segmentColor(height));
                canvas.drawRect(seg, fillPaint);
            } else {
                canvas.drawRect(seg, trackPaint);
            }
        }
    }

    private int segmentColor(int heightFromBottom) {
        if (heightFromBottom >= 11) return Color.parseColor("#FF5252");
        if (heightFromBottom >= 9) return Color.parseColor("#FFAA00");
        return Color.parseColor("#00E676");
    }
}