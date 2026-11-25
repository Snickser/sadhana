import ru.rolyc.sadhana.Report;
import ru.rolyc.sadhana.ReportJson;

import java.util.ArrayList;
import java.util.List;

/** JVM-тесты чистой логики экспорта/импорта (без Android). */
public class ReportJsonTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testRoundTrip();
        testBareArray();
        testSingleObject();
        testForeignApp();
        testBadDate();
        testDedupeInsideFile();
        testTolerantTypes();
        testBrokenJson();
        testEscapes();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    // ---------- tests ----------

    static void testRoundTrip() {
        List<Report> in = new ArrayList<Report>();
        in.add(report("2026-11-23", 16, 30, true, true, true, true, true, "пуджа, уборка", "Харе Кришна!\nДжапа шла хорошо"));
        in.add(report("2026-11-22", 8, 0, false, true, false, true, false, "", ""));
        in.add(report("2026-11-21", 0, 120, false, false, false, false, false, "кафе \"Гаура\"", "цитата: «ШБ 1.2.6»"));

        String json = ReportJson.toJson(in, 1732300000000L);
        List<Report> out = ReportJson.fromJson(json);

        check("roundtrip: count", out.size() == 3);
        for (int i = 0; i < in.size(); i++) {
            Report a = in.get(i);
            Report b = out.get(i);
            check("roundtrip date #" + i, a.date.equals(b.date));
            check("roundtrip rounds #" + i, a.rounds == b.rounds);
            check("roundtrip reading #" + i, a.readingMin == b.readingMin);
            check("roundtrip flags #" + i, a.wakeEarly == b.wakeEarly && a.sleepEarly == b.sleepEarly
                    && a.mangalaArati == b.mangalaArati && a.lecture == b.lecture && a.prasadam == b.prasadam);
            check("roundtrip service #" + i, a.service.equals(b.service));
            check("roundtrip notes #" + i, a.notes.equals(b.notes));
            check("roundtrip updated #" + i, a.updatedAt == b.updatedAt);
        }
    }

    static void testBareArray() {
        String json = "[{\"date\":\"2026-01-05\",\"rounds\":16},{\"date\":\"2026-01-06\",\"rounds\":17}]";
        List<Report> out = ReportJson.fromJson(json);
        check("bare array: count", out.size() == 2);
        check("bare array: rounds", out.get(1).rounds == 17);
    }

    static void testSingleObject() {
        String json = "{\"date\":\"2026-02-01\",\"rounds\":4,\"notes\":\"один день\"}";
        List<Report> out = ReportJson.fromJson(json);
        check("single object: count", out.size() == 1);
        check("single object: notes", "один день".equals(out.get(0).notes));
    }

    static void testForeignApp() {
        try {
            ReportJson.fromJson("{\"app\":\"other-app\",\"reports\":[]}");
            check("foreign app: должен был упасть", false);
        } catch (IllegalArgumentException e) {
            check("foreign app: отказ", e.getMessage().contains("другого приложения"));
        }
    }

    static void testBadDate() {
        try {
            ReportJson.fromJson("{\"reports\":[{\"date\":\"23.11.2026\"}]}");
            check("bad date: должен был упасть", false);
        } catch (IllegalArgumentException e) {
            check("bad date: отказ", e.getMessage().contains("ГГГГ-ММ-ДД"));
        }
        try {
            ReportJson.fromJson("{\"reports\":[{\"rounds\":16}]}");
            check("missing date: должен был упасть", false);
        } catch (IllegalArgumentException e) {
            check("missing date: отказ", e.getMessage().contains("date"));
        }
        try {
            ReportJson.fromJson("{\"reports\":[{\"date\":\"2026-13-40\"}]}");
            check("bad month: должен был упасть", false);
        } catch (IllegalArgumentException e) {
            check("bad month: отказ", e.getMessage().contains("ГГГГ-ММ-ДД"));
        }
    }

    static void testDedupeInsideFile() {
        String json = "{\"reports\":["
                + "{\"date\":\"2026-03-01\",\"rounds\":5,\"updated_at\":100},"
                + "{\"date\":\"2026-03-01\",\"rounds\":16,\"updated_at\":200}]}";
        List<Report> out = ReportJson.fromJson(json);
        check("dedupe: одна запись", out.size() == 1);
        check("dedupe: свежая победила", out.get(0).rounds == 16);

        String json2 = "{\"reports\":["
                + "{\"date\":\"2026-03-01\",\"rounds\":5,\"updated_at\":300},"
                + "{\"date\":\"2026-03-01\",\"rounds\":16,\"updated_at\":200}]}";
        List<Report> out2 = ReportJson.fromJson(json2);
        check("dedupe: старая не перебила", out2.get(0).rounds == 5);
    }

    static void testTolerantTypes() {
        String json = "{\"reports\":[{\"date\":\"2026-04-01\",\"rounds\":\"16\",\"readingMin\":\"45\","
                + "\"wake_early\":\"да\",\"sleepEarly\":1,\"mangala_arati\":\"true\",\"notes\":\"текст\"}]}";
        List<Report> out = ReportJson.fromJson(json);
        Report r = out.get(0);
        check("tolerant: rounds из строки", r.rounds == 16);
        check("tolerant: readingMin camelCase", r.readingMin == 45);
        check("tolerant: 'да' как boolean", r.wakeEarly);
        check("tolerant: 1 как boolean", r.sleepEarly);
        check("tolerant: 'true' как boolean", r.mangalaArati);
    }

    static void testBrokenJson() {
        String[] bad = {
                "{\"reports\":[{\"date\":\"2026-01-01\"}",
                "{\"reports\":}",
                "не json вообще",
                "",
        };
        for (int i = 0; i < bad.length; i++) {
            try {
                ReportJson.fromJson(bad[i]);
                check("broken #" + i + ": должен был упасть", false);
            } catch (RuntimeException e) {
                check("broken #" + i + ": отказ", true);
            }
        }
    }

    static void testEscapes() {
        Report r = report("2026-05-05", 16, 10, true, false, false, false, true,
                "строка с \"кавычками\" и \\ обратным слэшем", "таб\tздесь\nновая строка, юникод: ॐ Кришна");
        List<Report> in = new ArrayList<Report>();
        in.add(r);
        List<Report> out = ReportJson.fromJson(ReportJson.toJson(in, 1L));
        check("escapes: service", r.service.equals(out.get(0).service));
        check("escapes: notes", r.notes.equals(out.get(0).notes));
        // unicode-escape в файле тоже должно разбирать
        List<Report> u = ReportJson.fromJson("{\"reports\":[{\"date\":\"2026-05-06\",\"notes\":\"\\u0915\\u0943\"}]}");
        check("escapes: \\u разобран", "कृ".equals(u.get(0).notes));
    }

    // ---------- helpers ----------

    static Report report(String date, int rounds, int reading, boolean wake, boolean sleep,
                         boolean arati, boolean lecture, boolean prasadam, String service, String notes) {
        Report r = new Report();
        r.date = date;
        r.rounds = rounds;
        r.readingMin = reading;
        r.wakeEarly = wake;
        r.sleepEarly = sleep;
        r.mangalaArati = arati;
        r.lecture = lecture;
        r.prasadam = prasadam;
        r.service = service;
        r.notes = notes;
        r.updatedAt = 1732300000000L;
        return r;
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
