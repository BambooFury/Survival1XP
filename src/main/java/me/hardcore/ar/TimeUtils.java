package me.hardcore.ar;

public final class TimeUtils {

    private TimeUtils() {}

    /**
     * Форматирует время в секундах в читаемый формат HH:MM:SS
     */
    public static String formatTime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Форматирует число с разделителями тысяч
     */
    public static String formatNumber(double number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000);
        }
        return String.format("%.0f", number);
    }

    /**
     * Форматирует прогресс с одним знаком после запятой
     */
    public static String formatProgress(double progress) {
        return String.format("%.1f", Math.min(progress, 100.0));
    }
}
