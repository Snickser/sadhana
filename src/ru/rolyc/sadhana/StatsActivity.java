package ru.rolyc.sadhana;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** Экран графиков: круги джапы, регулярные пункты, чтение — по дням за период. */
public class StatsActivity extends Activity {

    private static final int PERIOD_DEFAULT = 30;

    private int period = PERIOD_DEFAULT;

    private ReportsDb db;
    private TextView summary;
    private BarChartView chartRounds;
    private BarChartView chartPoints;
    private BarChartView chartReading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        db = new ReportsDb(this);
        summary = (TextView) findViewById(R.id.statsSummary);
        chartRounds = (BarChartView) findViewById(R.id.chartRounds);
        chartPoints = (BarChartView) findViewById(R.id.chartPoints);
        chartReading = (BarChartView) findViewById(R.id.chartReading);

        findViewById(R.id.btnStatsBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        wirePeriod(R.id.btnPeriod7, 7);
        wirePeriod(R.id.btnPeriod30, 30);
        wirePeriod(R.id.btnPeriod90, 90);

        render();
    }

    private void wirePeriod(int id, final int days) {
        Button b = (Button) findViewById(id);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = days;
                render();
            }
        });
    }

    private void render() {
        StatsData d = StatsData.build(db.all(), StatsData.todayIso(), period);

        chartRounds.setData(d.rounds, d.labels, d.present, Report.TARGET_ROUNDS,
                "Круги джапы", "цель " + Report.TARGET_ROUNDS);
        chartPoints.setData(d.points, d.labels, d.present, 5,
                "Регулярные пункты", "из 5");
        chartReading.setData(d.readingMin, d.labels, d.present, 0,
                "Чтение, минут", "мин");
        chartPoints.setBarColor(0xFF6D8C4E);
        chartReading.setBarColor(0xFF7E57C2);

        highlightPeriodButtons();

        int avgPointsWhole = d.avgPoints10 / 10;
        int avgPointsFrac = d.avgPoints10 % 10;
        String avgPoints = d.daysFilled > 0
                ? avgPointsWhole + (avgPointsFrac > 0 ? "." + avgPointsFrac : "")
                : "—";

        StringBuilder sb = new StringBuilder();
        sb.append("Период: ").append(d.days).append(" дн. · заполнено ").append(d.daysFilled);
        sb.append("\nСреднее кругов: ").append(d.daysFilled > 0 ? String.valueOf(d.avgRounds) : "—");
        sb.append(" · всего: ").append(d.totalRounds);
        sb.append("\nЛучший день: ").append(d.maxRounds).append(" круг.");
        sb.append("\nСредний балл пунктов: ").append(avgPoints).append(" из 5");
        sb.append("\nПодряд до сегодня: ").append(d.streak).append(" дн.");
        summary.setText(sb.toString());
    }

    private void highlightPeriodButtons() {
        int[] ids = {R.id.btnPeriod7, R.id.btnPeriod30, R.id.btnPeriod90};
        int[] days = {7, 30, 90};
        for (int i = 0; i < ids.length; i++) {
            Button b = (Button) findViewById(ids[i]);
            boolean active = days[i] == period;
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    active ? 0xFFE65100 : 0xFFFFE0B2));
            b.setTextColor(active ? 0xFFFFFFFF : 0xFFBF360C);
        }
    }
}
