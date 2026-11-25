package ru.rolyc.sadhana;

/**
 * Один отчёт по личной садхане за календарный день.
 * Чистый Java без зависимостей от Android — чтобы логику можно было тестировать на JVM.
 */
public class Report {

    public static final int TARGET_ROUNDS = 16;

    public String date = "";        // yyyy-MM-dd
    public int rounds = 0;          // круги джапы
    public int readingMin = 0;      // чтение, минут
    public boolean wakeEarly = false;    // подъём до 5:00
    public boolean sleepEarly = false;   // отбой до 22:00
    public boolean mangalaArati = false; // мангала-арати
    public boolean lecture = false;      // лекция / класс
    public boolean prasadam = false;     // только прасад
    public String service = "";     // служение
    public String notes = "";       // заметки
    public long updatedAt = 0L;     // время последнего изменения, millis

    public Report() {
    }

    /** Сколько из пяти регулярных пунктов отмечено. */
    public int checkedCount() {
        int n = 0;
        if (wakeEarly) n++;
        if (sleepEarly) n++;
        if (mangalaArati) n++;
        if (lecture) n++;
        if (prasadam) n++;
        return n;
    }

    /** Простой балл дня: круги до цели + отмеченные пункты. */
    public int score() {
        int s = checkedCount();
        if (rounds >= TARGET_ROUNDS) s++;
        if (readingMin > 0) s++;
        return s; // максимум 7
    }

    public Report copy() {
        Report r = new Report();
        r.date = date;
        r.rounds = rounds;
        r.readingMin = readingMin;
        r.wakeEarly = wakeEarly;
        r.sleepEarly = sleepEarly;
        r.mangalaArati = mangalaArati;
        r.lecture = lecture;
        r.prasadam = prasadam;
        r.service = service;
        r.notes = notes;
        r.updatedAt = updatedAt;
        return r;
    }
}
