package de.nll.lager;

import java.math.BigDecimal;

public final class Rules {
    private Rules() {}
    public static String quantity(String raw) {
        if (raw == null || !raw.trim().matches("[0-9]{1,9}([.,][0-9]{1,3})?")) {
            throw new IllegalArgumentException("Menge: maximal 9 Stellen und 3 Nachkommastellen.");
        }
        BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
        if (value.signum() <= 0) throw new IllegalArgumentException("Die Menge muss größer als 0 sein.");
        return value.stripTrailingZeros().toPlainString();
    }
    public static String required(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException(label + " fehlt.");
        if (value.length() > 200) throw new IllegalArgumentException(label + ": maximal 200 Zeichen.");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) throw new IllegalArgumentException(label + " enthält Steuerzeichen.");
        }
        return value;
    }
    public static void locations(String from, String to) {
        if (required(from, "Von Lager").equalsIgnoreCase(required(to, "Nach Lager"))) {
            throw new IllegalArgumentException("Von Lager und Nach Lager müssen verschieden sein.");
        }
    }
    /** Spreadsheet-safe quoted field. Every field is quoted, including embedded CR/LF and quotes. */
    public static String csv(String value) {
        String text = value == null ? "" : value;
        int start = 0;
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
        String trimmed = text.substring(start);
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) text = "'" + text;
        if (!text.isEmpty() && (text.charAt(0) == '\t' || text.charAt(0) == '\r' || text.charAt(0) == '\n')) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
