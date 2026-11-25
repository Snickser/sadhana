package ru.rolyc.sadhana;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Данные для графиков: ряды по дням за период. Чистый Java — тестируется на JVM.
 *
 * Ряды идут от старого дня к новому, последний элемент — endIso.
 * Пропущенный день даёт 0 и present=false, чтобы на графике было видно пробел.
 */
public final class StatsData {

    public final String[] labels;      // "дд.ММ" для подписи под столбиком
    public final String[] isoDates;    // точные даты
    public final int[] rounds;         // круги джапы
    public final int[] readingMin;     // чтение, минут
    public final int[] points;         // отмечено регулярных пунктов, 0..5
    public final boolean[] present;    // был ли отчёт за день

    public final int days;             // длина периода
    public final int daysFilled;       // сколько дней с отчётом
    public final int totalRounds;      // сумма кругов за период
    public final int avgRounds;        // среднее по дням с отчётом
    public final int maxRounds;        // лучший день по кругам
    public final int streak;           // сколько дней подряд заполнено до последнего дня
    public final int avgPoints10;      // средний балл пунктов ×10 (для дробного среднего)

    private StatsData(int days, String[] labels, String[] isoDates, int[] rounds,
                      int[] readingMin, int[] points, boolean[] present) {
        this.days = days;
        this.labels = labels;
        this.isoDates = isoDates;
        this.rounds = rounds;
        this.readingMin = readingMin;
        this.points = points;
        this.present = present;

        int filled = 0;
        int sumRounds = 0;
        int best = 0;
        int sumPoints = 0;
        for (int i = 0; i < days; i++) {
            if (present[i]) {
                filled++;
                sumRounds += rounds[i];
                sumPoints += points[i];
            }
            if (rounds[i] > best) best = rounds[i];
        }
        this.daysFilled = filled;
        this.totalRounds = sumRounds;
        this.maxRounds = best;
        this.avgRounds = filled > 0 ? Math.round((float) sumRounds / filled) : 0;
        this.avgPoints10 = filled > 0 ? Math.round(10f * sumPoints / filled) : 0;

        int run = 0;
        for (int i = days - 1; i >= 0; i--) {
            if (present[i]) run++;
            else break;
        }
        this.streak = run;
    }

    /**
     * Строит ряды за последние {@code days} дней включительно по {@code endIso}.
     */
    public static StatsData build(List<Report> reports, String endIso, int days) {
        if (days < 1) days = 1;
        Map<String, Report> byDate = new HashMap<String, Report>();
        if (reports != null) {
            for (int i = 0; i < reports.size(); i++) {
                Report r = reports.get(i);
                if (r != null && r.date != null) byDate.put(r.date, r);
            }
        }

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat shortFmt = new SimpleDateFormat("dd.MM", Locale.US);
        Date end = parse(iso, endIso);

        String[] labels = new String[days];
        String[] isoDates = new String[days];
        int[] rounds = new int[days];
        int[] reading = new int[days];
        int[] points = new int[days];
        boolean[] present = new boolean[days];

        Calendar c = Calendar.getInstance();
        c.setTime(end);
        c.add(Calendar.DAY_OF_YEAR, -(days - 1)); // самый старый день периода

        for (int i = 0; i < days; i++) {
            String d = iso.format(c.getTime());
            isoDates[i] = d;
            labels[i] = shortFmt.format(c.getTime());
            Report r = byDate.get(d);
            if (r != null) {
                present[i] = true;
                rounds[i] = r.rounds;
                reading[i] = r.readingMin;
                points[i] = r.checkedCount();
            }
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return new StatsData(days, labels, isoDates, rounds, reading, points, present);
    }

    public static String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static Date parse(SimpleDateFormat fmt, String s) {
        try {
            Date d = fmt.parse(s);
            if (d != null) return d;
        } catch (ParseException ignored) {
            // ниже — сегодняшний день
        }
        return new Date();
    }
}
