package com.prison.core.util;

import com.prison.core.PrisonPlugin;
import org.bukkit.entity.Player;

/**
 * Форматує тривалість (мілісекунди) у зручний для читання вигляд, з
 * абревіатурами одиниць (дні/години/хвилини), взятими з lang-файлів
 * (time-days/time-hours/time-minutes) - тому кожна мова може мати свої
 * скорочення без правок коду.
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    public static String format(PrisonPlugin plugin, Player player, long millis) {
        long totalMinutes = millis / 60_000L;
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        String d = plugin.getMessages().get(player, "time-days");
        String h = plugin.getMessages().get(player, "time-hours");
        String m = plugin.getMessages().get(player, "time-minutes");

        if (days > 0) return days + d + " " + hours + h;
        if (hours > 0) return hours + h + " " + minutes + m;
        return minutes + m;
    }
}
