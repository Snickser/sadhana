import ru.rolyc.sadhana.ReminderTime;

import java.util.Calendar;

/** JVM-тесты расчёта времени напоминания. */
public class ReminderTimeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        sameDay();
        nextDay();
        exactMoment();
        clamping();
        label();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, hour, minute, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static void sameDay() {
        long now = at(2026, 11, 23, 9, 0);
        long next = ReminderTime.nextTrigger(now, 21, 0);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(next);
        check("21:00 ещё не прошло → сегодня",
                c.get(Calendar.DAY_OF_MONTH) == 23 && c.get(Calendar.HOUR_OF_DAY) == 21
                        && c.get(Calendar.MINUTE) == 0);
        check("время в будущем", next > now);
    }

    static void nextDay() {
        long now = at(2026, 11, 23, 22, 30);
        long next = ReminderTime.nextTrigger(now, 21, 0);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(next);
        check("21:00 уже прошло → завтра",
                c.get(Calendar.DAY_OF_MONTH) == 24 && c.get(Calendar.HOUR_OF_DAY) == 21);
        check("завтрашнее время в будущем", next > now);
        check("ровно через сутки от сегодняшних 21:00",
                next - at(2026, 11, 23, 21, 0) == 24L * 60 * 60 * 1000);
    }

    static void exactMoment() {
        long now = at(2026, 11, 23, 21, 0);
        long next = ReminderTime.nextTrigger(now, 21, 0);
        check("ровно в момент срабатывания → следующие сутки", next - now == 24L * 60 * 60 * 1000);

        long before = at(2026, 11, 23, 20, 59);
        long n2 = ReminderTime.nextTrigger(before, 21, 0);
        check("за минуту до → через минуту", n2 - before == 60L * 1000);
    }

    static void clamping() {
        long now = at(2026, 11, 23, 12, 0);
        long n = ReminderTime.nextTrigger(now, 99, 99);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(n);
        check("99:99 → 23:59", c.get(Calendar.HOUR_OF_DAY) == 23 && c.get(Calendar.MINUTE) == 59);
        long m = ReminderTime.nextTrigger(now, -5, -5);
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(m);
        check("-5:-5 → 00:00 завтра", c2.get(Calendar.HOUR_OF_DAY) == 0
                && c2.get(Calendar.MINUTE) == 0 && c2.get(Calendar.DAY_OF_MONTH) == 24);
    }

    static void label() {
        check("label(21,0) = 21:00", "21:00".equals(ReminderTime.label(21, 0)));
        check("label(6,5) = 06:05", "06:05".equals(ReminderTime.label(6, 5)));
        check("label(23,59) = 23:59", "23:59".equals(ReminderTime.label(23, 59)));
    }

    static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  ok   " + name);
        } else {
            failed++;
            System.out.println("  FAIL " + name);
        }
    }
}
