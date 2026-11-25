package ru.rolyc.sadhana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки телефона будильники сбрасываются — ставим заново. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ReminderScheduler.apply(context);
        }
    }
}
