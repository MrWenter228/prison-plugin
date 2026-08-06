package com.prison.core.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Дозволяє використовувати у мовних файлах:
 *   &#FF0000&#0000FFтекст   - два hex-коди підряд (у звичайному форматі &#RRGGBB)
 *                              запускають плавний перехід кольору по символах наступного
 *                              відрізка тексту, аж до наступного &-коду або кінця рядка
 *   &#RRGGBB                 - одиночний hex-код без пари одразу після нього -
 *                              звичайний суцільний hex-колір (без градієнта)
 *
 * Обидва формати конвертуються у легасі §x§R§R§G§G§B§B-послідовності через
 * net.md_5.bungee.api.ChatColor.of(Color), які підтримує клієнт Minecraft 1.16+.
 * Виклик GradientUtil.apply() слід робити ДО ChatColor.translateAlternateColorCodes('&', ...),
 * щоб залишкові '&'-коди форматування (&l, &n тощо) відпрацювали як завжди.
 */
public final class GradientUtil {

    // Два &#HEX підряд, одразу за ними - відрізок тексту без '&' (до наступного коду чи кінця рядка)
    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("&#([0-9a-fA-F]{6})&#([0-9a-fA-F]{6})([^&]+)");
    private static final Pattern SOLID_HEX_PATTERN =
            Pattern.compile("&#([0-9a-fA-F]{6})");

    private GradientUtil() {
    }

    public static String apply(String input) {
        if (input == null || input.isEmpty()) return input;
        return applySolidHex(applyGradients(input));
    }

    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        if (!matcher.find()) return input;

        StringBuilder result = new StringBuilder();
        int last = 0;
        matcher.reset();
        while (matcher.find()) {
            result.append(input, last, matcher.start());
            result.append(buildGradient(matcher.group(3), "#" + matcher.group(1), "#" + matcher.group(2)));
            last = matcher.end();
        }
        result.append(input.substring(last));
        return result.toString();
    }

    private static String applySolidHex(String input) {
        Matcher matcher = SOLID_HEX_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            Color color = Color.decode("#" + matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(ChatColor.of(color).toString()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Розфарбовує кожен символ text у плавний перехід від fromHex до toHex. */
    private static String buildGradient(String text, String fromHex, String toHex) {
        int length = text.length();
        if (length == 0) return text;

        Color from = Color.decode(fromHex);
        Color to = Color.decode(toHex);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            // Пропускаємо легасі формат-коди (&l, &n, &o, &k, &r) - вони не мають отримувати колір,
            // інакше зіб'ється підрахунок індексу градієнта на видимих символах.
            if (c == '&' && i + 1 < length) {
                sb.append(c).append(text.charAt(i + 1));
                i++;
                continue;
            }

            double ratio = (length == 1) ? 0.0 : (double) i / (double) (length - 1);
            int r = interpolate(from.getRed(), to.getRed(), ratio);
            int g = interpolate(from.getGreen(), to.getGreen(), ratio);
            int b = interpolate(from.getBlue(), to.getBlue(), ratio);

            sb.append(ChatColor.of(new Color(r, g, b)).toString()).append(c);
        }
        return sb.toString();
    }

    private static int interpolate(int from, int to, double ratio) {
        return (int) Math.round(from + (to - from) * ratio);
    }
}
