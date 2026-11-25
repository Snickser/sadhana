package ru.rolyc.sadhana;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Минимальный JSON: писатель + разборщик. Без внешних зависимостей и без android.util,
 * чтобы класс компилировался и тестировался на обычной JVM.
 *
 * parse() возвращает: LinkedHashMap<String,Object> | ArrayList<Object> | String | Double | Boolean | null
 */
public final class MiniJson {

    private MiniJson() {
    }

    // ---------------- writer ----------------

    public static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ---------------- parser ----------------

    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("Пустой файл");
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (!p.eof()) throw p.err("лишние данные после JSON");
        return v;
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
            // BOM
            if (s.length() > 0 && s.charAt(0) == '\uFEFF') i = 1;
        }

        boolean eof() {
            return i >= s.length();
        }

        IllegalArgumentException err(String msg) {
            return new IllegalArgumentException("JSON: " + msg + " (позиция " + i + ")");
        }

        void skipWs() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
                else break;
            }
        }

        Object value() {
            skipWs();
            if (eof()) throw err("неожиданный конец файла");
            char c = s.charAt(i);
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return number();
                    throw err("неожиданный символ '" + c + "'");
            }
        }

        void expect(String word) {
            if (!s.startsWith(word, i)) throw err("ожидалось " + word);
            i += word.length();
        }

        Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            i++; // {
            skipWs();
            if (!eof() && s.charAt(i) == '}') { i++; return map; }
            while (true) {
                skipWs();
                if (eof() || s.charAt(i) != '"') throw err("ожидался ключ");
                String key = string();
                skipWs();
                if (eof() || s.charAt(i) != ':') throw err("ожидалось ':'");
                i++;
                Object v = value();
                map.put(key, v);
                skipWs();
                if (eof()) throw err("объект не закрыт");
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == '}') { i++; return map; }
                throw err("ожидалось ',' или '}'");
            }
        }

        List<Object> array() {
            List<Object> list = new ArrayList<Object>();
            i++; // [
            skipWs();
            if (!eof() && s.charAt(i) == ']') { i++; return list; }
            while (true) {
                Object v = value();
                list.add(v);
                skipWs();
                if (eof()) throw err("массив не закрыт");
                char c = s.charAt(i);
                if (c == ',') { i++; continue; }
                if (c == ']') { i++; return list; }
                throw err("ожидалось ',' или ']'");
            }
        }

        String string() {
            i++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) throw err("строка не закрыта");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (eof()) throw err("незавершённый escape");
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw err("плохой \\u escape");
                        String hex = s.substring(i, i + 4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw err("плохой \\u escape: " + hex);
                        }
                        i += 4;
                        break;
                    default:
                        throw err("неизвестный escape \\" + e);
                }
            }
        }

        Double number() {
            int start = i;
            if (!eof() && s.charAt(i) == '-') i++;
            while (!eof()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') i++;
                else break;
            }
            String num = s.substring(start, i);
            try {
                return Double.valueOf(num);
            } catch (NumberFormatException ex) {
                throw err("плохое число: " + num);
            }
        }
    }
}
