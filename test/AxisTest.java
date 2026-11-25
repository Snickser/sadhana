import ru.rolyc.sadhana.Axis;

/** JVM-тесты шкалы графика. */
public class AxisTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        basics();
        grid();
        headroom();
        smallValues();
        bigValues();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static void basics() {
        check("0 → 1", Axis.niceMax(0) == 1);
        check("1 → 1", Axis.niceMax(1) == 1);
        check("3 → 3", Axis.niceMax(3) == 3);
        check("5 → 5", Axis.niceMax(5) == 5);
        check("7 → 8", Axis.niceMax(7) == 8);
        check("16 → 16", Axis.niceMax(16) == 16);
        check("17 → 20", Axis.niceMax(17) == 20);
        check("отрицательное → 1", Axis.niceMax(-5) == 1);
    }

    static void grid() {
        int[] g = Axis.gridLevels(16);
        check("сетка 16: 0/8/16", g[0] == 0 && g[1] == 8 && g[2] == 16);
        int[] g2 = Axis.gridLevels(6);
        check("сетка 6: 0/3/6", g2[0] == 0 && g2[1] == 3 && g2[2] == 6);
        int[] g3 = Axis.gridLevels(1);
        check("сетка 1: 0/0/1", g3[0] == 0 && g3[1] == 0 && g3[2] == 1);
    }

    static void headroom() {
        check("пункты 5 из 5 → верх 6 (а не 8)", Axis.niceMaxWithHeadroom(5, 5) == 6);
        check("пункты 4, цель 5 → верх 6", Axis.niceMaxWithHeadroom(4, 5) == 6);
        check("круги 16, цель 16 → верх 20", Axis.niceMaxWithHeadroom(16, 16) == 20);
        check("максимум 25, цель 16 → верх 30", Axis.niceMaxWithHeadroom(25, 16) == 30);
        check("максимум 10, цель 16 → верх 20", Axis.niceMaxWithHeadroom(10, 16) == 20);
        check("максимум 17, цели нет → верх 20", Axis.niceMaxWithHeadroom(17, 0) == 20);
        check("пусто, цели нет → 1", Axis.niceMaxWithHeadroom(0, 0) == 1);
        check("пусто, цель 5 → верх 6", Axis.niceMaxWithHeadroom(0, 5) == 6);
        check("чтение 50 мин → верх 60", Axis.niceMaxWithHeadroom(50, 0) == 60);
    }

    static void smallValues() {
        boolean tight = true;
        for (int v = 1; v <= 30; v++) {
            int top = Axis.niceMaxWithHeadroom(v, 0);
            if (top > v * 1.5 + 1) {
                tight = false;
                System.out.println("    слишком высоко: " + v + " → " + top);
            }
        }
        check("1..30: верх не выше данных в 1.5 раза", tight);

        boolean above = true;
        for (int v = 1; v <= 300; v++) {
            if (Axis.niceMaxWithHeadroom(v, 0) <= v) above = false;
        }
        check("верх всегда строго выше максимума", above);
    }

    static void bigValues() {
        check("1000 → 1200", Axis.niceMaxWithHeadroom(1000, 0) == 1200);
        check("1200 → 1500", Axis.niceMaxWithHeadroom(1200, 0) == 1500);
        check("12345 → 13000", Axis.niceMaxWithHeadroom(12345, 0) == 13000);
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
