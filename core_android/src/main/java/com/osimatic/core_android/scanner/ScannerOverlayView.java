package com.osimatic.core_android.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class ScannerOverlayView extends View {

    private static final float VIEWFINDER_RATIO = 0.72f;
    private static final float CORNER_RATIO = 0.12f;
    private static final float CORNER_STROKE_DP = 4f;
    private static final float CORNER_RADIUS_DP = 2f;

    private final Paint overlayPaint = new Paint();
    private final Paint clearPaint = new Paint();
    private final Paint cornerPaint = new Paint();
    private final RectF viewfinderRect = new RectF();

    public ScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // nécessaire pour que PorterDuff CLEAR fonctionne
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        overlayPaint.setColor(0xAA000000);
        overlayPaint.setStyle(Paint.Style.FILL);

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setStyle(Paint.Style.FILL);

        float density = getResources().getDisplayMetrics().density;
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(CORNER_STROKE_DP * density);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        float size = Math.min(width, height) * VIEWFINDER_RATIO;
        float left = (width - size) / 2f;
        float top = (height - size) / 2f;
        viewfinderRect.set(left, top, left + size, top + size);

        float radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CORNER_RADIUS_DP, getResources().getDisplayMetrics());

        canvas.drawRect(0, 0, width, height, overlayPaint);
        canvas.drawRoundRect(viewfinderRect, radius, radius, clearPaint);

        drawCorners(canvas, size * CORNER_RATIO);
    }

    private void drawCorners(Canvas canvas, float len) {
        float l = viewfinderRect.left;
        float t = viewfinderRect.top;
        float r = viewfinderRect.right;
        float b = viewfinderRect.bottom;

        // haut-gauche
        canvas.drawLine(l, t + len, l, t, cornerPaint);
        canvas.drawLine(l, t, l + len, t, cornerPaint);
        // haut-droit
        canvas.drawLine(r - len, t, r, t, cornerPaint);
        canvas.drawLine(r, t, r, t + len, cornerPaint);
        // bas-gauche
        canvas.drawLine(l, b - len, l, b, cornerPaint);
        canvas.drawLine(l, b, l + len, b, cornerPaint);
        // bas-droit
        canvas.drawLine(r - len, b, r, b, cornerPaint);
        canvas.drawLine(r, b, r, b - len, cornerPaint);
    }
}