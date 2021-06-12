package rekov.graduate.autoprotocol.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Класс для представления даты и времени в определенном общем формате
 */
public class DateTimeFormatter {
    private static final String TIME_PATTERN = "HH:mm:ss.SSS";
    private static final String DATE_PATTERN = "dd.MM.yyyy";
    private static final String TIME_ZONE_GMT_ID = "GMT";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_PATTERN, Locale.ENGLISH);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);

    static {
        timeFormat.setTimeZone(java.util.TimeZone.getTimeZone(TIME_ZONE_GMT_ID));
    }

    /**
     * Форматирование времени
     *
     * @param dateTime время в UNIX-формате
     * @return строка с форматированным временем
     */
    public static String formatTime(long dateTime) {
        return timeFormat.format(dateTime);
    }

    /**
     * Форматирование даты
     *
     * @param dateTime время в UNIX-формате
     * @return строка с форматированной датой
     */
    public static String formatDate(long dateTime) {
        return dateFormat.format(dateTime);
    }

    /**
     * Используемый шаблон для форматирования времени
     *
     * @return формат-строка
     */
    public static String getTimePattern() {
        return TIME_PATTERN;
    }

    /**
     * Используемая временаня зона для форматирования даты и времени
     *
     * @return идентификатор временной зоны (например, GMT)
     */
    public static String getTimeZone() {
        return TIME_ZONE_GMT_ID;
    }
}
