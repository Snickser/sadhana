package ru.rolyc.sadhana;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Формат файла экспорта/импорта.
 *
 * {
 *   "app": "sadhana",
 *   "format": 1,
 *   "exported_at": 1732300000000,
 *   "reports": [ { "date": "2026-11-23", "rounds": 16, ... }, ... ]
 * }
 *
 * Импорт принимает также просто массив отчётов и объект с одним отчётом.
 * Чистый Java — тестируется на JVM.
 */
public final class ReportJson {

    public static final String APP_ID = "sadhana";
    public static final int FORMAT = 1;

    private ReportJson() {
    }

    public static String toJson(List<Report> reports, long exportedAt) {
        StringBuilder sb = new StringBuilder(256 + reports.size() * 220);
        sb.append("{\n");
        sb.append("  \"app\": ").append(MiniJson.escape(APP_ID)).append(",\n");
        sb.append("  \"format\": ").append(FORMAT).append(",\n");
        sb.append("  \"exported_at\": ").append(exportedAt).append(",\n");
        sb.append("  \"count\": ").append(reports.size()).append(",\n");
        sb.append("  \"reports\": [");
        for (int k = 0; k < reports.size(); k++) {
            Report r = reports.get(k);
            sb.append(k == 0 ? "\n" : ",\n");
            sb.append("    {\n");
            sb.append("      \"date\": ").append(MiniJson.escape(r.date)).append(",\n");
            sb.append("      \"rounds\": ").append(r.rounds).append(",\n");
            sb.append("      \"reading_min\": ").append(r.readingMin).append(",\n");
            sb.append("      \"wake_early\": ").append(r.wakeEarly).append(",\n");
            sb.append("      \"sleep_early\": ").append(r.sleepEarly).append(",\n");
            sb.append("      \"mangala_arati\": ").append(r.mangalaArati).append(",\n");
            sb.append("      \"lecture\": ").append(r.lecture).append(",\n");
            sb.append("      \"prasadam\": ").append(r.prasadam).append(",\n");
            sb.append("      \"service\": ").append(MiniJson.escape(r.service == null ? "" : r.service)).append(",\n");
            sb.append("      \"notes\": ").append(MiniJson.escape(r.notes == null ? "" : r.notes)).append(",\n");
            sb.append("      \"updated_at\": ").append(r.updatedAt).append("\n");
            sb.append("    }");
        }
        if (!reports.isEmpty()) sb.append("\n  ");
        sb.append("]\n}\n");
        return sb.toString();
    }

    /** Разбирает файл. Бросает IllegalArgumentException с понятным текстом при ошибке. */
    public static List<Report> fromJson(String text) {
        Object root = MiniJson.parse(text);
        List<Object> items = new ArrayList<Object>();

        if (root instanceof List) {
            items.addAll((List<?>) root);
        } else if (root instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) root;
            Object app = map.get("app");
            if (app instanceof String && !APP_ID.equals(app)) {
                throw new IllegalArgumentException("Файл от другого приложения: " + app);
            }
            Object reports = map.get("reports");
            if (reports instanceof List) {
                items.addAll((List<?>) reports);
            } else if (map.containsKey("date")) {
                items.add(map);
            } else {
                throw new IllegalArgumentException("В файле нет списка \"reports\"");
            }
        } else {
            throw new IllegalArgumentException("Неизвестный формат файла");
        }

        List<Report> out = new ArrayList<Report>(items.size());
        Map<String, Integer> seen = new LinkedHashMap<String, Integer>();
        for (int k = 0; k < items.size(); k++) {
            Object o = items.get(k);
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("Элемент " + (k + 1) + " — не объект");
            }
            Report r = one((Map<?, ?>) o, k + 1);
            Integer dup = seen.get(r.date);
            if (dup != null) {
                // внутри файла дата повторилась — оставляем более свежую запись
                Report prev = out.get(dup.intValue());
                if (r.updatedAt >= prev.updatedAt) out.set(dup.intValue(), r);
            } else {
                seen.put(r.date, Integer.valueOf(out.size()));
                out.add(r);
            }
        }
        return out;
    }

    private static Report one(Map<?, ?> m, int index) {
        Report r = new Report();
        Object date = m.get("date");
        if (!(date instanceof String)) {
            throw new IllegalArgumentException("Отчёт " + index + ": нет поля \"date\"");
        }
        r.date = ((String) date).trim();
        if (!isIsoDate(r.date)) {
            throw new IllegalArgumentException("Отчёт " + index + ": дата должна быть в виде ГГГГ-ММ-ДД, а не \"" + r.date + "\"");
        }
        r.rounds = intOf(m, "rounds", 0);
        r.readingMin = intOf(m, "reading_min", intOf(m, "readingMin", 0));
        r.wakeEarly = boolOf(m, "wake_early", boolOf(m, "wakeEarly", false));
        r.sleepEarly = boolOf(m, "sleep_early", boolOf(m, "sleepEarly", false));
        r.mangalaArati = boolOf(m, "mangala_arati", boolOf(m, "mangalaArati", false));
        r.lecture = boolOf(m, "lecture", false);
        r.prasadam = boolOf(m, "prasadam", false);
        r.service = strOf(m, "service", "");
        r.notes = strOf(m, "notes", "");
        r.updatedAt = longOf(m, "updated_at", longOf(m, "updatedAt", 0L));
        if (r.rounds < 0) r.rounds = 0;
        if (r.readingMin < 0) r.readingMin = 0;
        return r;
    }

    public static boolean isIsoDate(String s) {
        if (s == null || s.length() != 10) return false;
        for (int i = 0; i < 10; i++) {
            char c = s.charAt(i);
            if (i == 4 || i == 7) {
                if (c != '-') return false;
            } else if (c < '0' || c > '9') {
                return false;
            }
        }
        int m = Integer.parseInt(s.substring(5, 7));
        int d = Integer.parseInt(s.substring(8, 10));
        return m >= 1 && m <= 12 && d >= 1 && d <= 31;
    }

    private static int intOf(Map<?, ?> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Double) return (int) Math.round(((Double) v).doubleValue());
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        if (v instanceof Boolean) return ((Boolean) v).booleanValue() ? 1 : 0;
        return def;
    }

    private static long longOf(Map<?, ?> m, String key, long def) {
        Object v = m.get(key);
        if (v instanceof Double) return ((Double) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong(((String) v).trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static boolean boolOf(Map<?, ?> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v instanceof Double) return ((Double) v).doubleValue() != 0d;
        if (v instanceof String) {
            String s = ((String) v).trim().toLowerCase();
            return s.equals("true") || s.equals("1") || s.equals("да") || s.equals("yes");
        }
        return def;
    }

    private static String strOf(Map<?, ?> m, String key, String def) {
        Object v = m.get(key);
        if (v instanceof String) return (String) v;
        if (v instanceof Double || v instanceof Boolean) return String.valueOf(v);
        return def;
    }
}
