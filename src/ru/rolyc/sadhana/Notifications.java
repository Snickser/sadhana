package ru.rolyc.sadhana;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Показ уведомления-напоминания. */
public final class Notifications {

    public static final String CHANNEL_ID = "sadhana_reminder";
    public static final int ID = 1001;

    private Notifications() {
    }

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Ежедневное напоминание", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Напоминание заполнить отчёт по садхане");
        nm.createNotificationChannel(ch);
    }

    /** Показывает напоминание. Текст зависит от того, заполнен ли отчёт за сегодня. */
    public static void showReminder(Context ctx) {
        ensureChannel(ctx);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        boolean filled;
        ReportsDb db = new ReportsDb(ctx);
        try {
            filled = db.byDate(today) != null;
        } catch (RuntimeException e) {
            filled = false;
        }

        String title = filled ? "Отчёт за сегодня уже есть" : "Заполните отчёт по садхане";
        String text = filled
                ? "Можно дополнить: круги, чтение, служение."
                : "Круги джапы, чтение и регулярные пункты за сегодня.";

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(ctx, 0, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(ctx, CHANNEL_ID);
        } else {
            b = new Notification.Builder(ctx);
            b.setPriority(Notification.PRIORITY_DEFAULT);
        }
        b.setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);

        try {
            nm.notify(ID, b.build());
        } catch (SecurityException e) {
            // пользователь не дал разрешение на уведомления — молча выходим
        }
    }

    public static void clear(Context ctx) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(ID);
    }
}
