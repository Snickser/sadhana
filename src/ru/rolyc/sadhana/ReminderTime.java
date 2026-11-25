package ru.rolyc.sadhana;

import java.util.Calendar;

/**
 * Расчёт времени следующего напоминания. Чистый Java — тестируется на JVM.
 */
public final class ReminderTime {

    public static final int DEFAULT_HOUR = 21;
    public static final int DEFAULT_MINUTE = 0;

    private ReminderTime() {
    }

    /**
     * Ближайший момент hour:minute строго в будущем относительно nowMillis.
     * Если сегодняшнее время уже прошло (или совпало) — переносится на завтра.
     */
    public static long nextTrigger(long nowMillis, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nowMillis);
        c.set(Calendar.HOUR_OF_DAY, clampHour(hour));
        c.set(Calendar.MINUTE, clampMinute(minute));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long candidate = c.getTimeInMillis();
        if (candidate <= nowMillis) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            candidate = c.getTimeInMillis();
        }
        return candidate;
    }

    public static String label(int hour, int minute) {
        return two(clampHour(hour)) + ":" + two(clampMinute(minute));
    }

    public static int clampHour(int h) {
        if (h < 0) return 0;
        if (h > 23) return 23;
        return h;
    }

    public static int clampMinute(int m) {
        if (m < 0) return 0;
        if (m > 59) return 59;
        return m;
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }
}
