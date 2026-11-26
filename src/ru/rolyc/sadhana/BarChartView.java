package ru.rolyc.sadhana;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Столбчатый график по дням: столбики, линия цели, подписи осей.
 * Нарисован вручную на Canvas — без библиотек и без AndroidX.
 */
public class BarChartView extends View {

    private int[] values = new int[0];
    private String[] labels = new String[0];
    private boolean[] present = new boolean[0];
    private int target = 0;
    private String title = "";
    private String unit = "";
    private int barColor = 0xFFC1440E;
    private int emptyBarColor = 0xFFEFE3CE;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path targetPath = new Path();

    private float density;

    public BarChartView(Context ctx) {
        super(ctx);
        init();
    }

    public BarChartView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        text.setColor(0xFF8A6A52);
        text.setTextSize(sp(9));
        grid.setColor(0xFFF2E7D3);
        grid.setStrokeWidth(dp(1));
        targetPaint.setColor(0xFFC8A15A);
        targetPaint.setStrokeWidth(dp(1.4f));
        targetPaint.setStyle(Paint.Style.STROKE);
        targetPaint.setPathEffect(new DashPathEffect(new float[]{dp(5), dp(4)}, 0));
    }

    /** @param present где false — дня без отчёта, столбик рисуется бледным */
    public void setData(int[] values, String[] labels, boolean[] present,
                        int target, String title, String unit) {
        this.values = values != null ? values : new int[0];
        this.labels = labels != null ? labels : new String[0];
        this.present = present != null ? present : new boolean[0];
        this.target = target;
        this.title = title != null ? title : "";
        this.unit = unit != null ? unit : "";
        invalidate();
    }

    public void setBarColor(int color) {
        this.barColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = resolveSize((int) dp(320), widthMeasureSpec);
        int h = resolveSize((int) dp(170), heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        float w = getWidth();
        float h = getHeight();

        // заголовок
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(0xFF6B1F1F);
        titlePaint.setTextSize(sp(13));
        titlePaint.setFakeBoldText(true);
        canvas.drawText(title, dp(4), dp(16), titlePaint);

        if (n == 0) {
            text.setColor(0xFF8A6A52);
            canvas.drawText("нет данных за период", dp(4), h / 2f, text);
            return;
        }

        float left = dp(30);
        float right = w - dp(4);
        float top = dp(28);
        float bottom = h - dp(16);
        float plotW = Math.max(1f, right - left);
        float plotH = Math.max(1f, bottom - top);

        int max = target;
        for (int i = 0; i < n; i++) if (values[i] > max) max = values[i];
        if (max <= 0) max = 1;
        // верх шкалы с запасом, чтобы столбик-максимум не упирался в рамку
        max = Axis.niceMaxWithHeadroom(max, target);

        // горизонтальная сетка с подписями (0, середина, верх)
        int[] levels = {0, max / 2, max};
        for (int k = 0; k < levels.length; k++) {
            float y = bottom - plotH * levels[k] / max;
            canvas.drawLine(left, y, right, y, grid);
            String s = String.valueOf(levels[k]);
            canvas.drawText(s, dp(2), y + sp(3), text);
        }

        // линия цели
        if (target > 0 && target <= max) {
            float y = bottom - plotH * target / max;
            targetPath.reset();
            targetPath.moveTo(left, y);
            targetPath.lineTo(right, y);
            canvas.drawPath(targetPath, targetPaint);
        }

        // столбики
        float slot = plotW / n;
        float barW = Math.max(dp(1.5f), slot * 0.62f);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2f;
            boolean has = i < present.length && present[i];
            int v = values[i];
            if (v <= 0) {
                // нулевой день — тонкая светлая метка на оси, чтобы пробел был виден
                paint.setColor(emptyBarColor);
                canvas.drawRect(cx - barW / 2f, bottom - dp(1f), cx + barW / 2f, bottom, paint);
                continue;
            }
            float barH = plotH * v / max;
            paint.setColor(has ? barColor : emptyBarColor);
            canvas.drawRect(cx - barW / 2f, bottom - barH, cx + barW / 2f, bottom, paint);
        }

        // подписи дат: не больше шести, чтобы не слипались
        int step = Math.max(1, (int) Math.ceil(n / 6.0));
        text.setColor(0xFF8A6A52);
        for (int i = 0; i < n; i += step) {
            float cx = left + slot * i + slot / 2f;
            String s = i < labels.length ? labels[i] : "";
            float tw = text.measureText(s);
            canvas.drawText(s, cx - tw / 2f, bottom + sp(11), text);
        }

        // подпись единицы измерения справа внизу
        if (unit.length() > 0) {
            float tw = text.measureText(unit);
            text.setColor(0xFF8A6A52);
            canvas.drawText(unit, right - tw, top - dp(4), text);
        }
    }

    private float dp(float v) {
        return v * density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
