package zc246_zl345_co232_mw756;

import java_cup.runtime.Symbol;

public class Lexer {
    private static final String[] terminalNames = new String[]{
            "EOF", "error", "id", "string", "character", "integer", "true", "false", "integer 9223372036854775808", "int", "bool", "if", "else",
            "while", "return", "use", "+", "-", "*", "*>>", "/", "%", "!", "<", "<=", ">", ">=", "&", "|", "==", "!=",
            "=", "[", "]", "(", ")", "{", "}", ":", ",", ";", "_", "length", "-", "null", "record", "dot", "break"
    };

    public static String stringOfToken(Symbol s) {
        String type = terminalNames[s.sym];
        StringBuilder str = new StringBuilder();

        str.append((s.left) + ":" + (s.right) + " " + type);
        if (type == "integer" || type == "id") str.append(" ").append(s.value);
        else if (type == "character") str.append(" ").append(escape((Long) s.value));
        else if (type == "string") str.append(" ").append(escapeAll((Long[]) s.value));
        return str.append("\n").toString();
    }

    public static String escape(Long i) {
        if (i == 9) return "\\t";
        else if (i == 10) return "\\n";
        else if (i == 34) return "\\\"";
        else if (i == 39) return "\\'";
        else if (i == 92) return "\\\\";
        else if (i >= 128) return "\\x{" + Long.toHexString(i) + "}";
        else return "" + (char) (long) i;
    }

    public static String escapeAll(Long[] s) {
        StringBuilder sb = new StringBuilder();
        for (long i : s) {
            sb.append(escape(i));
        }
        return sb.toString();
    }
}
