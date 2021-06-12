package rekov.graduate.autoprotocol.configurations;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Объект "Конфигурация события"
 * Используется для записи информации по событию в итоговый протокол
 * Также используется для управления параметрами события (количество участников, задержки синхронизации)
 */
public class EventConfiguration implements Serializable {
    public static final String DIR = "eventconfs";
    private static final String CURRENT_CONFIG_NAME = "currenteventconf";
    public static final String FILE_EXTENSION = ".apc";
    private static final String MOBILE_BLOCK_START = "%MOBILE_START%";
    private static final String MOBILE_BLOCK_END = "%MOBILE_END%";
    private static final String KEY_VALUE_DELIMITER = "=";
    private static final String KEY_MAX_PARTICIPANT = "MAX_PARTICIPANT";
    private static final String KEY_AUTO_SYNC_DELAY = "AUTO_SYNC_DELAY";
    private static final String KEY_MANUAL_SYNC_DELAY = "MANUAL_SYNC_DELAY";
    private static final String KEY_LAPS_COUNT = "LAPS_COUNT";
    private static final String KEY_CHECKPOINTS_COUNT = "CHECKPOINTS_COUNT";
    private static final String KEY_EVENT_NAME = "EVENT_NAME";
    private int maxParticipant = 0;
    private int autoSyncDelay = 1;
    private int manualSyncDelay = 10;
    private int lapsCount = 1;
    private int checkPointsCount = 1;
    private String eventName = "New event";
    private static ApplicationFileManager applicationFileManager;

    /**
     * Создание конфигурации доступно только через Builder
     */
    private EventConfiguration() {
    }

    /**
     * Отображение значений объекта Конфигурация события в соответствующие поля на форме
     *
     * @return набор пар {идентификатор_поля -> значение объекта}
     */
    public HashMap<Integer, String> getFields() {
        HashMap<Integer, String> resultMap = new HashMap<>();
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__EVENT_NAME, eventName);
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__MAX_PARTICIPANT, String.valueOf(maxParticipant));
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__AUTO_SYNC_DELAY, String.valueOf(autoSyncDelay));
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__MANUAL_SYNC_DELAY, String.valueOf(manualSyncDelay));
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__LAPS_COUNT, String.valueOf(lapsCount));
        resultMap.put(R.id.EVENT_CONFIGURATION_FIELD__CHECKPOINTS_COUNT, String.valueOf(checkPointsCount));
        return resultMap;
    }

    /**
     * Отображение значений редактируемых полей на форме в значения объекта Конфигурация события
     *
     * @param fieldsMap набор пар {идентификатор_поля -> значение объекта}
     * @return флаг успешной операции отображения
     */
    public boolean setFields(HashMap<Integer, String> fieldsMap) {
        try {
            eventName = fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__EVENT_NAME);
            maxParticipant = Integer.parseInt(Objects.requireNonNull(fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__MAX_PARTICIPANT)));
            autoSyncDelay = Integer.parseInt(Objects.requireNonNull(fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__AUTO_SYNC_DELAY)));
            manualSyncDelay = Integer.parseInt(Objects.requireNonNull(fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__MANUAL_SYNC_DELAY)));
            lapsCount = Integer.parseInt(Objects.requireNonNull(fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__LAPS_COUNT)));
            checkPointsCount = Integer.parseInt(Objects.requireNonNull(fieldsMap.get(R.id.EVENT_CONFIGURATION_FIELD__CHECKPOINTS_COUNT)));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public int getAutoSyncDelay() {
        return autoSyncDelay;
    }

    public int getManualSyncDelay() {
        return manualSyncDelay;
    }

    public int getMaxParticipant() {
        return maxParticipant;
    }

    public String getEventName() {
        return eventName;
    }

    public int getCheckPointsCount() {
        return checkPointsCount;
    }

    public int getLapsCount() {
        return lapsCount;
    }

    /**
     * Применение Конфигурации события
     * (по сути запись объекта в специальный файл)
     *
     * @param eventConfiguration объект Конфигурации события, который требуется применить
     * @return флаг успешной операции применения конфигурации
     * @see #getCurrent(Context) получение примененной конфигурации
     */
    public static boolean apply(@NonNull EventConfiguration eventConfiguration) {
        return applicationFileManager.writeObjectFile(CURRENT_CONFIG_NAME, eventConfiguration);
    }

    /**
     * Получение текущей Конфигурации события
     * (по сути чтение объекта из специального файла)
     *
     * @param context контекст приложения (необходим для связывания файлового менеджера приложения)
     * @return объект Конфигурации события (может быть null, если конфигурация отсутствует)
     * @see #apply(EventConfiguration) метод, c помощью которого применяется текущая конфигурация
     */
    @Nullable
    public static EventConfiguration getCurrent(Context context) {
        applicationFileManager = ApplicationFileManager.getInstance(context);
        return (EventConfiguration) applicationFileManager.readObjectFile(CURRENT_CONFIG_NAME);
    }

    /**
     * Запись объекта Конфигурация события в файл в строковом формате
     * Необходимо для передачи конфигураций между устройствами и для хранения предыдущих конфигураций
     *
     * @param fileName имя файла (расширение будет добавлено автоматически)
     * @return флаг успешной операции записи файла
     */
    public boolean writeFile(String fileName) {
        return applicationFileManager.writeFile(DIR, fileName + FILE_EXTENSION, getStringData());
    }

    /**
     * Сериализация объекта Конфигурация события в строку
     *
     * @return строковое представление объекта в согласованном формате
     */
    public String getStringData() {
        return MOBILE_BLOCK_START +
                '\n' + KEY_EVENT_NAME + KEY_VALUE_DELIMITER + eventName +
                '\n' + KEY_MAX_PARTICIPANT + KEY_VALUE_DELIMITER + maxParticipant +
                '\n' + KEY_AUTO_SYNC_DELAY + KEY_VALUE_DELIMITER + autoSyncDelay +
                '\n' + KEY_MANUAL_SYNC_DELAY + KEY_VALUE_DELIMITER + manualSyncDelay +
                '\n' + KEY_LAPS_COUNT + KEY_VALUE_DELIMITER + lapsCount +
                '\n' + KEY_CHECKPOINTS_COUNT + KEY_VALUE_DELIMITER + checkPointsCount +
                '\n' + MOBILE_BLOCK_END;
    }

    /**
     * Builder, с помощью которого создаются объекты Конфигурация события
     */
    public static class Builder {
        private final EventConfiguration eventConfiguration = new EventConfiguration();

        /**
         * Инициализация нового объекта Конфигурация события со стандратными значениями
         *
         * @param context контекст приложения (необходим для связывания файлового менеджера приложения)
         */
        public Builder(Context context) {
            applicationFileManager = ApplicationFileManager.getInstance(context);
        }

        /**
         * Инициализация нового объекта Конфигурация события из файла специального формата
         *
         * @param from    файл специального формата, из которого нужно создать Конфигурацию события
         * @param context контекст приложения (необходим для связывания файлового менеджера приложения)
         * @throws NumberFormatException если не удалось преобразовать значение из файла в числовое поле объекта
         */
        public Builder(File from, Context context) throws NumberFormatException {
            applicationFileManager = ApplicationFileManager.getInstance(context);
            /*
             *  Из файла читается только "мобильный блок"
             *  Значения объекта представлены строками вида KEY=VALUE
             */
            ArrayList<String> data = applicationFileManager.readFile(from, MOBILE_BLOCK_START, MOBILE_BLOCK_END);
            for (String dataString : data) {
                int delimiterIndex = dataString.indexOf(KEY_VALUE_DELIMITER);
                String key = dataString.substring(0, delimiterIndex);
                String value = dataString.substring(delimiterIndex + 1);
                switch (key) {
                    case KEY_MAX_PARTICIPANT:
                        eventConfiguration.maxParticipant = Integer.parseInt(value);
                        break;
                    case KEY_AUTO_SYNC_DELAY:
                        eventConfiguration.autoSyncDelay = Integer.parseInt(value);
                        break;
                    case KEY_MANUAL_SYNC_DELAY:
                        eventConfiguration.manualSyncDelay = Integer.parseInt(value);
                        break;
                    case KEY_LAPS_COUNT:
                        eventConfiguration.lapsCount = Integer.parseInt(value);
                        break;
                    case KEY_CHECKPOINTS_COUNT:
                        eventConfiguration.checkPointsCount = Integer.parseInt(value);
                        break;
                    case KEY_EVENT_NAME:
                        eventConfiguration.eventName = value;
                        break;
                }
            }
        }

        /**
         * Создание объекта Конфигурации события
         *
         * @return созданный объект конфигурации события
         */
        public EventConfiguration create() {
            return eventConfiguration;
        }
    }
}
