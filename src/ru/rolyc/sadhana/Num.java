package ru.rolyc.sadhana;

/**
 * Разбор числовых полей формы. Чистый Java — тестируется на JVM.
 *
 * Терпим к тому, что присылает клавиатура: пробелы, неразрывные пробелы,
 * запятые, точки, ведущие нули, арабско-индийские и полноширинные цифры.
 * Отрицательные и мусорные значения превращаются в 0 — «кругов −3» не бывает.
 */
public final class Num {

    public static final int MAX_ROUNDS = 999;
    public static final int MAX_MINUTES = 1440;

    private Num() {
    }

    /** Все цифры из строки, в порядке появления. Пустая строка или мусор → 0. */
    public static int parse(String raw) {
        if (raw == null) return 0;
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            int d = digitValue(raw.charAt(i));
            if (d >= 0) digits.append((char) ('0' + d));
        }
        if (digits.length() == 0) return 0;
        if (digits.length() > 9) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static int rounds(String raw) {
        return clamp(parse(raw), 0, MAX_ROUNDS);
    }

    public static int minutes(String raw) {
        return clamp(parse(raw), 0, MAX_MINUTES);
    }

    public static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /** Значение цифры для латинских, полноширинных и деванагарских/арабских начертаний; -1 — не цифра. */
    private static int digitValue(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= '\uFF10' && c <= '\uFF19') return c - '\uFF10'; // полноширинные
        if (c >= '\u0966' && c <= '\u096F') return c - '\u0966'; // деванагари
        if (c >= '\u0660' && c <= '\u0669') return c - '\u0660'; // арабско-индийские
        return -1;
    }

    /** Строка для поля ввода: пустая при нуле, чтобы не мешала подсказка. */
    public static String field(int value) {
        return value > 0 ? String.valueOf(value) : "";
    }
}
