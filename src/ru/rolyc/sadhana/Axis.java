package ru.rolyc.sadhana;

/** Расчёт шкалы графика. Чистый Java — тестируется на JVM. */
public final class Axis {

    /**
     * Допустимые «круглые» верхние значения. На малых числах шаги частые (3, 5, 6),
     * чтобы шкала не улетала вдвое выше данных: 5 из 5 больше не даёт 8.
     */
    private static final int[] STEPS = {
            1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 16, 20, 25, 30, 40, 50,
            60, 80, 100, 120, 150, 200, 250, 300, 400, 500, 600, 800,
            1000, 1200, 1500, 2000, 2500, 3000, 4000, 5000
    };

    private Axis() {
    }

    /** Наименьшее «круглое» число, не меньшее value. */
    public static int niceMax(int value) {
        if (value <= 1) return 1;
        for (int i = 0; i < STEPS.length; i++) {
            if (value <= STEPS[i]) return STEPS[i];
        }
        return ((value + 999) / 1000) * 1000;
    }

    /** Три уровня сетки: низ, середина, верх. */
    public static int[] gridLevels(int max) {
        int top = niceMax(max);
        return new int[]{0, top / 2, top};
    }

    /**
     * Верх шкалы для столбиков: строго выше и максимума, и цели — столбик не упирается
     * в рамку, но лишнего запаса нет. 5 → 6, 16 → 20, 25 → 30, 50 → 60.
     */
    public static int niceMaxWithHeadroom(int maxValue, int target) {
        int floor = maxValue > target ? maxValue : target;
        if (floor <= 0) return 1;
        return niceMax(floor + 1);
    }
}
