package com.prison.core.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Форматування сум у монетах: якщо значення менше 1000 - показується
 * дробове число (2 знаки після коми), якщо 1000 і більше - лише ціла
 * частина (без копійок), бо на таких сумах копійки не мають практичного
 * значення і лише захаращують інтерфейс.
 */
public final class CoinFormat {

    private static final DecimalFormatSymbols SYMBOLS = createSymbols();
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0", SYMBOLS);
    private static final DecimalFormat FRACTIONAL = new DecimalFormat("#,##0.00", SYMBOLS);
    private static final double THRESHOLD = 1000.0;

    private CoinFormat() {
    }

    private static DecimalFormatSymbols createSymbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' '); // Встановлюємо звичайний пробіл ('\u0020') замість нерозривного ('\u00A0')
        return symbols;
    }

    public static String format(double amount) {
        if (Math.abs(amount) < THRESHOLD) {
            return FRACTIONAL.format(amount);
        }
        return WHOLE.format(amount);
    }
}