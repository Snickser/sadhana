package ru.rolyc.sadhana;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Локальная база отчётов (SQLite, без сети). */
public class ReportsDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "sadhana.db";
    private static final int DB_VERSION = 1;
    public static final String TABLE = "reports";

    public ReportsDb(Context ctx) {
        super(ctx.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date TEXT NOT NULL UNIQUE,"
                + "rounds INTEGER NOT NULL DEFAULT 0,"
                + "reading_min INTEGER NOT NULL DEFAULT 0,"
                + "wake_early INTEGER NOT NULL DEFAULT 0,"
                + "sleep_early INTEGER NOT NULL DEFAULT 0,"
                + "mangala_arati INTEGER NOT NULL DEFAULT 0,"
                + "lecture INTEGER NOT NULL DEFAULT 0,"
                + "prasadam INTEGER NOT NULL DEFAULT 0,"
                + "service TEXT NOT NULL DEFAULT '',"
                + "notes TEXT NOT NULL DEFAULT '',"
                + "updated_at INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // схема v1 — миграций пока нет
    }

    /** Все отчёты, свежие сверху. */
    public List<Report> all() {
        List<Report> out = new ArrayList<Report>();
        Cursor c = getReadableDatabase().query(TABLE, null, null, null, null, null, "date DESC");
        try {
            while (c.moveToNext()) out.add(read(c));
        } finally {
            c.close();
        }
        return out;
    }

    public Report byDate(String date) {
        Cursor c = getReadableDatabase().query(TABLE, null, "date=?", new String[]{date}, null, null, null, "1");
        try {
            return c.moveToFirst() ? read(c) : null;
        } finally {
            c.close();
        }
    }

    /** Последний отчёт строго раньше указанной даты — из него заполняется новый день. */
    public Report previousBefore(String date) {
        Cursor c = getReadableDatabase().query(TABLE, null, "date<?", new String[]{date},
                null, null, "date DESC", "1");
        try {
            return c.moveToFirst() ? read(c) : null;
        } finally {
            c.close();
        }
    }

    public int count() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE, null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    /** Вставка или замена записи за ту же дату. */
    public void upsert(Report r) {
        if (r.updatedAt <= 0) r.updatedAt = System.currentTimeMillis();
        ContentValues cv = new ContentValues();
        cv.put("date", r.date);
        cv.put("rounds", r.rounds);
        cv.put("reading_min", r.readingMin);
        cv.put("wake_early", r.wakeEarly ? 1 : 0);
        cv.put("sleep_early", r.sleepEarly ? 1 : 0);
        cv.put("mangala_arati", r.mangalaArati ? 1 : 0);
        cv.put("lecture", r.lecture ? 1 : 0);
        cv.put("prasadam", r.prasadam ? 1 : 0);
        cv.put("service", r.service == null ? "" : r.service);
        cv.put("notes", r.notes == null ? "" : r.notes);
        cv.put("updated_at", r.updatedAt);
        getWritableDatabase().insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Импорт одной записи: не затирает более свежую локальную версию.
     *
     * @return "new" — добавлено, "updated" — заменено, "skipped" — пропущено
     */
    public String importOne(Report r) {
        Report existing = byDate(r.date);
        if (existing == null) {
            upsert(r);
            return "new";
        }
        long incoming = r.updatedAt;
        if (incoming > 0 && existing.updatedAt > 0 && incoming < existing.updatedAt) {
            return "skipped";
        }
        upsert(r);
        return "updated";
    }

    public void delete(String date) {
        getWritableDatabase().delete(TABLE, "date=?", new String[]{date});
    }

    /** Удаляет все отчёты. Возвращает число удалённых строк. */
    public int deleteAll() {
        return getWritableDatabase().delete(TABLE, "1", null);
    }

    private Report read(Cursor c) {
        Report r = new Report();
        r.date = c.getString(c.getColumnIndexOrThrow("date"));
        r.rounds = c.getInt(c.getColumnIndexOrThrow("rounds"));
        r.readingMin = c.getInt(c.getColumnIndexOrThrow("reading_min"));
        r.wakeEarly = c.getInt(c.getColumnIndexOrThrow("wake_early")) != 0;
        r.sleepEarly = c.getInt(c.getColumnIndexOrThrow("sleep_early")) != 0;
        r.mangalaArati = c.getInt(c.getColumnIndexOrThrow("mangala_arati")) != 0;
        r.lecture = c.getInt(c.getColumnIndexOrThrow("lecture")) != 0;
        r.prasadam = c.getInt(c.getColumnIndexOrThrow("prasadam")) != 0;
        r.service = c.getString(c.getColumnIndexOrThrow("service"));
        r.notes = c.getString(c.getColumnIndexOrThrow("notes"));
        r.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return r;
    }
}
