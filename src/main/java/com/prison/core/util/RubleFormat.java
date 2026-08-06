package com.prison.core.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Форматування сум у рублях - завжди лише ціле число, без копійок.
 * Рублі - донат-валюта фіксованих номіналів (бустери тощо), дробова
 * частина тут ніколи не має практичного значення.
 */
public final class RubleFormat {

    private static final DecimalFormatSymbols SYMBOLS = createSymbols();
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0", SYMBOLS);

    private RubleFormat() {
    }

    private static DecimalFormatSymbols createSymbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' '); // Замінюємо NBSP (\u00A0) на звичайний пробіл ('\u0020')
        return symbols;
    }

    public static String format(double amount) {
        return WHOLE.format(amount);
    }
}