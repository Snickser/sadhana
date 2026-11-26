package ru.rolyc.sadhana;

/**
 * Сравнение версий вида «3.7», «v3.10», «3.7.2».
 * Чистый Java — тестируется на JVM.
 */
public final class VersionCheck {

    private VersionCheck() {
    }

    /** Убирает префикс «v»/«V» и пробелы: «v3.7» → «3.7». */
    public static String tagToVersion(String tag) {
        if (tag == null) return "";
        String s = tag.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        return s.trim();
    }

    /** Числовые части версии: «3.10.2» → [3, 10, 2]. Мусор в части → 0. */
    public static int[] parse(String version) {
        String v = tagToVersion(version);
        if (v.length() == 0) return new int[]{0};
        String[] parts = v.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            StringBuilder digits = new StringBuilder();
            for (int k = 0; k < parts[i].length(); k++) {
                char c = parts[i].charAt(k);
                if (c >= '0' && c <= '9') digits.append(c);
                else break;               // «7-beta» → 7
            }
            try {
                out[i] = digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }

    /** -1 если a &lt; b, 0 если равны, 1 если a &gt; b. Недостающие части считаются нулями. */
    public static int compare(String a, String b) {
        int[] x = parse(a);
        int[] y = parse(b);
        int n = x.length > y.length ? x.length : y.length;
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? x[i] : 0;
            int yi = i < y.length ? y[i] : 0;
            if (xi < yi) return -1;
            if (xi > yi) return 1;
        }
        return 0;
    }

    /** Есть ли в сети версия новее установленной. */
    public static boolean isNewer(String remote, String current) {
        return compare(remote, current) > 0;
    }
}
