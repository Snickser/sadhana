package ru.rolyc.sadhana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Срабатывание будильника: показать напоминание и поставить следующее на завтра. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ReminderPrefs.enabled(context)) return;
        Notifications.showReminder(context);
        // setAndAllowWhileIdle — одноразовый, поэтому планируем следующий день.
        ReminderScheduler.scheduleNext(context);
    }
}
