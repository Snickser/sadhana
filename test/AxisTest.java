import ru.rolyc.sadhana.Axis;

/** JVM-тесты шкалы графика. */
public class AxisTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        basics();
        steps();
        grid();
        headroom();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static void basics() {
        check("0 → 1", Axis.niceMax(0) == 1);
        check("1 → 1", Axis.niceMax(1) == 1);
        check("2 → 2", Axis.niceMax(2) == 2);
        check("3 → 4", Axis.niceMax(3) == 4);
        check("16 → 16", Axis.niceMax(16) == 16);
        check("17 → 20", Axis.niceMax(17) == 20);
        check("25 → 25", Axis.niceMax(25) == 25);
        check("отрицательное → 1", Axis.niceMax(-5) == 1);
    }

    static void steps() {
        int[] probes = {5, 9, 12, 15, 21, 30, 33, 60, 99, 120, 250, 480, 900, 1200, 3000, 12345};
        boolean monotone = true;
        for (int i = 0; i < probes.length; i++) {
            int top = Axis.niceMax(probes[i]);
            if (top < probes[i]) {
                monotone = false;
                System.out.println("    шкала меньше значения: " + probes[i] + " → " + top);
            }
        }
        check("шкала всегда не меньше значения", monotone);
        check("1200 → 2000 (кратно 1000)", Axis.niceMax(1200) == 2000);
        check("12345 → 13000", Axis.niceMax(12345) == 13000);
    }

    static void grid() {
        int[] g = Axis.gridLevels(16);
        check("сетка 16: 0/8/16", g[0] == 0 && g[1] == 8 && g[2] == 16);
        int[] g2 = Axis.gridLevels(5);
        check("сетка 5: 0/4/8", g2[0] == 0 && g2[1] == 4 && g2[2] == 8);
        int[] g3 = Axis.gridLevels(1);
        check("сетка 1: 0/0/1", g3[0] == 0 && g3[1] == 0 && g3[2] == 1);
    }

    static void headroom() {
        check("максимум 25 → верх 32 (есть запас)", Axis.niceMaxWithHeadroom(25, 16) == 32);
        check("максимум 16, цель 16 → верх 20", Axis.niceMaxWithHeadroom(16, 16) == 20);
        check("максимум 10, цель 16 → верх 16", Axis.niceMaxWithHeadroom(10, 16) == 16);
        check("максимум 17, цель 0 → верх 20", Axis.niceMaxWithHeadroom(17, 0) == 20);
        check("максимум 5, цель 5 → верх 8", Axis.niceMaxWithHeadroom(5, 5) == 8);
        check("максимум 0, цель 0 → верх 1", Axis.niceMaxWithHeadroom(0, 0) == 1);
        check("максимум 12345 → верх больше значения",
                Axis.niceMaxWithHeadroom(12345, 0) > 12345);
        boolean alwaysAbove = true;
        for (int v = 0; v <= 200; v++) {
            if (Axis.niceMaxWithHeadroom(v, 0) <= v && v > 1) alwaysAbove = false;
        }
        check("верх всегда строго больше максимума", alwaysAbove);
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
