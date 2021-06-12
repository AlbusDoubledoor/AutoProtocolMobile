package rekov.graduate.autoprotocol.utils;

import android.util.Log;

/**
 * Обёртка для обобщенного логирования событий приложения
 */
public class Logger {
    private static final String APP_NAME = "AutoProtocol";

    /**
     * Логирование с уровнем ERROR
     *
     * @param src "источник" события
     * @param msg сообщение
     */
    public static void error(String src, String msg) {
        Log.e(APP_NAME, getSourcedMessage(src, msg));
    }

    /**
     * Логирование с уровнем DEBUG
     *
     * @param src "источник" события
     * @param msg сообщение
     */
    public static void debug(String src, String msg) {
        Log.d(APP_NAME, getSourcedMessage(src, msg));
    }

    /**
     * Формирование сообщения с указанием источника события
     *
     * @param src "источник" события
     * @param msg сообщение
     * @return форматированное сообщение с указанием источника события
     */
    private static String getSourcedMessage(String src, String msg) {
        return "[" + src + "] " + msg;
    }
}
