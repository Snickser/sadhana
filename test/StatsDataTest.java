import ru.rolyc.sadhana.Report;
import ru.rolyc.sadhana.StatsData;

import java.util.ArrayList;
import java.util.List;

/** JVM-тесты построения рядов для графиков. */
public class StatsDataTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        order();
        gaps();
        aggregates();
        streak();
        emptyInput();
        singleDay();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static Report r(String date, int rounds, int reading, boolean... flags) {
        Report rep = new Report();
        rep.date = date;
        rep.rounds = rounds;
        rep.readingMin = reading;
        rep.wakeEarly = flags.length > 0 && flags[0];
        rep.mangalaArati = flags.length > 1 && flags[1];
        rep.lecture = flags.length > 2 && flags[2];
        rep.prasadam = flags.length > 3 && flags[3];
        rep.sleepEarly = flags.length > 4 && flags[4];
        return rep;
    }

    static void order() {
        List<Report> list = new ArrayList<Report>();
        list.add(r("2026-11-21", 10, 15, true, true));
        list.add(r("2026-11-23", 16, 30, true, true, true));

        StatsData d = StatsData.build(list, "2026-11-23", 3);
        check("период 3 дня", d.days == 3);
        check("порядок: старый первым", "2026-11-21".equals(d.isoDates[0]));
        check("порядок: новый последним", "2026-11-23".equals(d.isoDates[2]));
        check("круги по дням", d.rounds[0] == 10 && d.rounds[2] == 16);
        check("чтение по дням", d.readingMin[0] == 15 && d.readingMin[2] == 30);
        check("пункты считаются", d.points[0] == 2 && d.points[2] == 3);
        check("подписи в формате дд.ММ", "21.11".equals(d.labels[0]) && "23.11".equals(d.labels[2]));
        check("present там, где есть отчёт", d.present[0] && d.present[2]);
    }

    static void gaps() {
        List<Report> list = new ArrayList<Report>();
        list.add(r("2026-11-21", 8, 0));
        list.add(r("2026-11-23", 12, 5));

        StatsData d = StatsData.build(list, "2026-11-23", 3);
        check("пропуск: нулевые круги", d.rounds[1] == 0);
        check("пропуск: present=false", !d.present[1]);
        check("заполнено 2 из 3", d.daysFilled == 2);
    }

    static void aggregates() {
        List<Report> list = new ArrayList<Report>();
        list.add(r("2026-11-20", 10, 0));
        list.add(r("2026-11-21", 20, 0));
        list.add(r("2026-11-22", 30, 0));

        StatsData d = StatsData.build(list, "2026-11-22", 5); // +2 пустых дня в начале
        check("всего кругов 60", d.totalRounds == 60);
        check("среднее по заполненным = 20", d.avgRounds == 20);
        check("максимум 30", d.maxRounds == 30);
        check("заполнено 3", d.daysFilled == 3);
        check("период 5 дней", d.days == 5);
    }

    static void streak() {
        List<Report> list = new ArrayList<Report>();
        list.add(r("2026-11-21", 5, 0));
        list.add(r("2026-11-22", 5, 0));
        list.add(r("2026-11-23", 5, 0));

        StatsData d = StatsData.build(list, "2026-11-23", 7);
        check("серия 3 дня", d.streak == 3);

        // сегодня пусто — серия обрывается
        StatsData d2 = StatsData.build(list, "2026-11-24", 7);
        check("серия обрывается на пустом дне", d2.streak == 0);

        // пропуск в середине не мешает серии в конце
        List<Report> list2 = new ArrayList<Report>();
        list2.add(r("2026-11-19", 5, 0));
        list2.add(r("2026-11-22", 5, 0));
        list2.add(r("2026-11-23", 5, 0));
        StatsData d3 = StatsData.build(list2, "2026-11-23", 7);
        check("серия 2 (считается от конца)", d3.streak == 2);
    }

    static void emptyInput() {
        StatsData d = StatsData.build(new ArrayList<Report>(), "2026-11-23", 7);
        check("пустой вход: период есть", d.days == 7);
        check("пустой вход: заполнено 0", d.daysFilled == 0);
        check("пустой вход: среднее 0", d.avgRounds == 0);
        check("пустой вход: серия 0", d.streak == 0);
        check("пустой вход: все present=false", !d.present[0] && !d.present[6]);

        StatsData n = StatsData.build(null, "2026-11-23", 7);
        check("null-вход не падает", n.days == 7 && n.daysFilled == 0);
    }

    static void singleDay() {
        List<Report> list = new ArrayList<Report>();
        list.add(r("2026-11-23", 16, 20));
        StatsData d = StatsData.build(list, "2026-11-23", 1);
        check("период 1 день", d.days == 1 && d.isoDates[0].equals("2026-11-23"));
        check("один день: круги", d.rounds[0] == 16 && d.streak == 1);
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
