import ru.rolyc.sadhana.UpdateChecker;
import ru.rolyc.sadhana.VersionCheck;

/** JVM-тесты сравнения версий и разбора ответа GitHub (без сети). */
public class VersionTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        tags();
        compareNumeric();
        newer();
        githubResponse();
        brokenResponse();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static void tags() {
        check("tagToVersion(\"v3.7\") = 3.7", "3.7".equals(VersionCheck.tagToVersion("v3.7")));
        check("tagToVersion(\"V3.10\") = 3.10", "3.10".equals(VersionCheck.tagToVersion("V3.10")));
        check("tagToVersion(\" 3.7 \") = 3.7", "3.7".equals(VersionCheck.tagToVersion(" 3.7 ")));
        check("tagToVersion(null) = пусто", "".equals(VersionCheck.tagToVersion(null)));
    }

    static void compareNumeric() {
        check("3.7 = 3.7", VersionCheck.compare("3.7", "3.7") == 0);
        check("3.10 > 3.7 (не строковое сравнение)", VersionCheck.compare("3.10", "3.7") == 1);
        check("3.7 < 3.10", VersionCheck.compare("3.7", "3.10") == -1);
        check("3.7 = 3.7.0", VersionCheck.compare("3.7", "3.7.0") == 0);
        check("3.7.1 > 3.7", VersionCheck.compare("3.7.1", "3.7") == 1);
        check("10.0 > 9.9", VersionCheck.compare("10.0", "9.9") == 1);
        check("v3.8 > 3.7 (префикс не мешает)", VersionCheck.compare("v3.8", "3.7") == 1);
        check("мусор = 0.0", VersionCheck.compare("abc", "0.0") == 0);
        check("3.7-beta = 3.7", VersionCheck.compare("3.7-beta", "3.7") == 0);
    }

    static void newer() {
        check("3.8 новее 3.7", VersionCheck.isNewer("3.8", "3.7"));
        check("3.7 не новее 3.7", !VersionCheck.isNewer("3.7", "3.7"));
        check("3.6 не новее 3.7", !VersionCheck.isNewer("3.6", "3.7"));
        check("v3.10 новее 3.7", VersionCheck.isNewer("v3.10", "3.7"));
    }

    static void githubResponse() {
        String json = "{\n"
                + "  \"tag_name\": \"v3.8\",\n"
                + "  \"name\": \"Садхана 3.8\",\n"
                + "  \"html_url\": \"https://github.com/Snickser/sadhana/releases/tag/v3.8\",\n"
                + "  \"assets\": [\n"
                + "    {\"name\": \"sadhana-3.8.apk\",\n"
                + "     \"browser_download_url\": \"https://github.com/Snickser/sadhana/releases/download/v3.8/sadhana-3.8.apk\"}\n"
                + "  ]\n"
                + "}";
        UpdateChecker.Result r = UpdateChecker.parse(json);
        check("ответ разобран", r.isOk());
        check("тег = v3.8", "v3.8".equals(r.tag));
        check("ссылка на страницу релиза", r.pageUrl != null && r.pageUrl.contains("/tag/v3.8"));
        check("версия новее 3.7", VersionCheck.isNewer(r.tag, "3.7"));

        // без html_url — берём ссылку на apk
        String noPage = "{\"tag_name\":\"v3.9\",\"assets\":[{\"browser_download_url\":\"https://x/y.apk\"}]}";
        UpdateChecker.Result r2 = UpdateChecker.parse(noPage);
        check("ссылка на apk как запасная", r2.isOk() && "https://x/y.apk".equals(r2.pageUrl));
    }

    static void brokenResponse() {
        UpdateChecker.Result r1 = UpdateChecker.parse("{\"message\":\"Not Found\"}");
        check("нет tag_name — ошибка", !r1.isOk() && r1.error != null);

        UpdateChecker.Result r2 = UpdateChecker.parse("не json");
        check("не JSON — ошибка, без падения", !r2.isOk());

        UpdateChecker.Result r3 = UpdateChecker.parse("{\"tag_name\":\"\"}");
        check("пустой tag — ошибка", !r3.isOk());

        UpdateChecker.Result r4 = UpdateChecker.parse("[]");
        check("массив вместо объекта — ошибка", !r4.isOk());
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
