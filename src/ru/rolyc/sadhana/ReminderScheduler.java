package ru.rolyc.sadhana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Планирование ежедневного напоминания. */
public final class ReminderScheduler {

    public static final String ACTION_REMIND = "ru.rolyc.sadhana.REMIND";
    private static final int REQUEST_CODE = 42;

    private ReminderScheduler() {
    }

    /** Применить текущие настройки: поставить будильник или снять его. */
    public static void apply(Context ctx) {
        if (ReminderPrefs.enabled(ctx)) {
            scheduleNext(ctx);
        } else {
            cancel(ctx);
        }
    }

    /** Поставить будильник на ближайшее время из настроек. */
    public static void scheduleNext(Context ctx) {
        if (!ReminderPrefs.enabled(ctx)) return;
        long at = ReminderTime.nextTrigger(System.currentTimeMillis(),
                ReminderPrefs.hour(ctx), ReminderPrefs.minute(ctx));
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = pending(ctx);
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                // Работает и в режиме экономии энергии, спецразрешений не требует.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            // на некоторых прошивках будильники запрещены — приложение продолжает работать
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pending(ctx));
    }

    private static PendingIntent pending(Context ctx) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction(ACTION_REMIND);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQUEST_CODE, i, flags);
    }
}
