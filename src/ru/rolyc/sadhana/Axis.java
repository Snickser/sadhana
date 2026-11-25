package ru.rolyc.sadhana;

/** Расчёт шкалы графика. Чистый Java — тестируется на JVM. */
public final class Axis {

    private Axis() {
    }

    /** Верх шкалы: 1,2,4,8,10,16,20,25,32,50,100,150,200,300,500,1000, дальше кратно 1000. */
    public static int niceMax(int value) {
        if (value <= 1) return 1;
        int[] steps = {1, 2, 4, 8, 10, 16, 20, 25, 32, 50, 100, 150, 200, 300, 500, 1000};
        for (int i = 0; i < steps.length; i++) {
            if (value <= steps[i]) return steps[i];
        }
        return ((value + 999) / 1000) * 1000;
    }

    /** Три уровня сетки: низ, середина, верх. */
    public static int[] gridLevels(int max) {
        int top = niceMax(max);
        return new int[]{0, top / 2, top};
    }

    /**
     * Верх шкалы для столбиков: не меньше значения и не меньше цели, но с запасом —
     * если максимум ровно совпал с шагом, берём следующий, чтобы столбик не упирался в рамку.
     */
    public static int niceMaxWithHeadroom(int maxValue, int target) {
        int floor = maxValue > target ? maxValue : target;
        int top = niceMax(floor);
        if (top <= maxValue) {
            top = nextStep(top);
        }
        return top;
    }

    /** Следующий шаг шкалы после текущего. */
    static int nextStep(int current) {
        int[] steps = {1, 2, 4, 8, 10, 16, 20, 25, 32, 50, 100, 150, 200, 300, 500, 1000};
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] > current) return steps[i];
        }
        return current + 1000 - (current % 1000);
    }
}
