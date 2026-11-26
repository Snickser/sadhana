package ru.rolyc.sadhana;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Запрос последней версии с GitHub Releases.
 * Единственный сетевой запрос в приложении — ничего, кроме номера версии, не отправляется
 * и не скачивается. Разбор ответа — тем же MiniJson, что и файлы импорта.
 */
public final class UpdateChecker {

    public static final String API_URL =
            "https://api.github.com/repos/Snickser/sadhana/releases/latest";
    private static final int TIMEOUT_MS = 12000;

    private UpdateChecker() {
    }

    /** Результат проверки: либо версия, либо текст ошибки. */
    public static final class Result {
        public final String tag;        // «v3.7», может быть null
        public final String pageUrl;    // ссылка на страницу релиза, может быть null
        public final String error;      // текст ошибки для пользователя, null если всё хорошо

        private Result(String tag, String pageUrl, String error) {
            this.tag = tag;
            this.pageUrl = pageUrl;
            this.error = error;
        }

        static Result ok(String tag, String pageUrl) {
            return new Result(tag, pageUrl, null);
        }

        static Result fail(String error) {
            return new Result(null, null, error);
        }

        public boolean isOk() {
            return error == null;
        }
    }

    /** Блокирующий запрос — вызывать из фонового потока. */
    public static Result fetchLatest() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "Sadhana-Android");
            int code = conn.getResponseCode();
            if (code == 404) {
                return Result.fail("Релизов пока нет — сравнивать не с чем.");
            }
            if (code == 403) {
                return Result.fail("GitHub ограничил частоту запросов. Попробуйте позже.");
            }
            if (code != 200) {
                return Result.fail("GitHub ответил кодом " + code + ".");
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            try {
                char[] buf = new char[4096];
                int n;
                while ((n = br.read(buf)) > 0) sb.append(buf, 0, n);
            } finally {
                br.close();
            }
            return parse(sb.toString());
        } catch (IOException e) {
            return Result.fail("Нет связи с GitHub: " + e.getMessage());
        } catch (RuntimeException e) {
            return Result.fail("Не удалось разобрать ответ: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Разбор ответа API. Вынесен отдельно, чтобы проверять на JVM без сети. */
    public static Result parse(String json) {
        Object root;
        try {
            root = MiniJson.parse(json);
        } catch (RuntimeException e) {
            // невалидный JSON от прокси, заглушки каптивного портала и т.п.
            return Result.fail("Ответ GitHub не разобран: " + e.getMessage());
        }
        if (!(root instanceof Map)) {
            return Result.fail("Неожиданный ответ GitHub.");
        }
        Map<?, ?> map = (Map<?, ?>) root;
        Object tag = map.get("tag_name");
        if (!(tag instanceof String) || ((String) tag).trim().length() == 0) {
            return Result.fail("В ответе нет номера версии.");
        }
        Object url = map.get("html_url");
        String page = url instanceof String ? (String) url : null;

        // есть ли apk среди файлов релиза — на случай, если ссылка на страницу не нужна
        Object assets = map.get("assets");
        if (assets instanceof List) {
            List<?> list = (List<?>) assets;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (!(item instanceof Map)) continue;
                Object dl = ((Map<?, ?>) item).get("browser_download_url");
                if (dl instanceof String && ((String) dl).endsWith(".apk")) {
                    page = page != null ? page : (String) dl;
                    break;
                }
            }
        }
        return Result.ok(((String) tag).trim(), page);
    }
}
