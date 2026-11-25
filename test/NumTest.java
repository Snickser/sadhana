import ru.rolyc.sadhana.Num;

/** JVM-тесты разбора числовых полей. */
public class NumTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        basic();
        junk();
        unicodeDigits();
        clamping();
        field();

        System.out.println("\n=== passed: " + passed + ", failed: " + failed + " ===");
        if (failed > 0) System.exit(1);
    }

    static void basic() {
        check("пустая строка → 0", Num.parse("") == 0);
        check("null → 0", Num.parse(null) == 0);
        check("\"16\" → 16", Num.parse("16") == 0 + 16);
        check("\" 16 \" → 16", Num.parse(" 16 ") == 16);
        check("\"016\" → 16", Num.parse("016") == 16);
        check("\"1 6\" → 16", Num.parse("1 6") == 16);
        check("неразрывный пробел", Num.parse("1\u00A06") == 16);
        check("узкий неразрывный пробел", Num.parse("1\u202F6") == 16);
    }

    static void junk() {
        check("\"16.\" → 16", Num.parse("16.") == 16);
        check("\"16,\" → 16", Num.parse("16,") == 16);
        check("\"-5\" → 5 (знак отброшен)", Num.parse("-5") == 5);
        check("буквы → 0", Num.parse("abc") == 0);
        check("\"1a6\" → 16", Num.parse("1a6") == 16);
        check("эмодзи → 0", Num.parse("🙏") == 0);
        check("слишком длинное → максимум int", Num.parse("123456789012") == Integer.MAX_VALUE);
        check("rounds(\"abc\") → 0", Num.rounds("abc") == 0);
    }

    static void unicodeDigits() {
        check("полноширинные \"１６\"", Num.parse("\uFF11\uFF16") == 16);
        check("деванагари \"१६\"", Num.parse("\u0967\u096C") == 16);
        check("арабско-индийские \"١٦\"", Num.parse("\u0661\u0666") == 16);
    }

    static void clamping() {
        check("rounds 5000 → 999", Num.rounds("5000") == 999);
        check("rounds 16 → 16", Num.rounds("16") == 16);
        check("minutes 99999 → 1440", Num.minutes("99999") == 1440);
        check("clamp(5,0,3) → 3", Num.clamp(5, 0, 3) == 3);
        check("clamp(-5,0,3) → 0", Num.clamp(-5, 0, 3) == 0);
    }

    static void field() {
        check("field(0) → \"\"", "".equals(Num.field(0)));
        check("field(16) → \"16\"", "16".equals(Num.field(16)));
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
