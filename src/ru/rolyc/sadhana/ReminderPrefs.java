package ru.rolyc.sadhana;

import android.content.Context;
import android.content.SharedPreferences;

/** Настройки напоминания: включено, час, минута. Хранятся в SharedPreferences. */
public final class ReminderPrefs {

    private static final String NAME = "sadhana_settings";
    private static final String KEY_ENABLED = "reminder_enabled";
    private static final String KEY_HOUR = "reminder_hour";
    private static final String KEY_MINUTE = "reminder_minute";

    private ReminderPrefs() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, false);
    }

    public static int hour(Context ctx) {
        return ReminderTime.clampHour(prefs(ctx).getInt(KEY_HOUR, ReminderTime.DEFAULT_HOUR));
    }

    public static int minute(Context ctx) {
        return ReminderTime.clampMinute(prefs(ctx).getInt(KEY_MINUTE, ReminderTime.DEFAULT_MINUTE));
    }

    public static void save(Context ctx, boolean enabled, int hour, int minute) {
        prefs(ctx).edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putInt(KEY_HOUR, ReminderTime.clampHour(hour))
                .putInt(KEY_MINUTE, ReminderTime.clampMinute(minute))
                .apply();
    }
}
