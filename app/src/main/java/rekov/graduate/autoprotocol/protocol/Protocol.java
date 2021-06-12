package rekov.graduate.autoprotocol.protocol;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import rekov.graduate.autoprotocol.timepoint.TimePoint;
import rekov.graduate.autoprotocol.utils.DateTimeFormatter;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Объект "Протокол", содержащий итоговый протокол события с метками времени и информацией по событию
 */
public class Protocol {
    public static final String FILE_EXTENSION = ".apd";
    public static final String DIR = "protocols";
    private final String data;
    private static ApplicationFileManager applicationFileManager;

    /**
     * Конструктор протокола доступен только через Builder
     *
     * @param data данные по протоколу в текстовом формате
     */
    private Protocol(String data) {
        this.data = data;
    }

    /**
     * Запись файла протокола в директорию внутри приложения
     *
     * @param fileName имя файла протокола (расширение добавляется автоматически)
     * @return флаг успешной операции записи файла
     */
    public boolean writeFile(String fileName) {
        return applicationFileManager.writeFile(DIR, fileName + FILE_EXTENSION, data);
    }

    /**
     * Builder объекта "Протокол", с помощью которого формируется данные протокола
     */
    public static class Builder {
        private final ArrayList<TimePoint> timePoints = new ArrayList<>();
        private final StringBuilder dataBuilder = new StringBuilder();
        private final ArrayList<String> metaElements = new ArrayList<>();
        private static final String META_KEY_TIME_PATTERN = "TIME_PATTERN";
        private static final String META_KEY_TIME_ZONE = "TIME_ZONE";
        private static final String META_KEY_EVENT_NAME = "EVENT_NAME";
        private static final String META_KEY_LAPS_COUNT = "LAPS_COUNT";
        private static final String META_KEY_CHECKPOINTS_COUNT = "CHECKPOINTS_COUNT";
        private static final String META_KEY_POINT_ID = "POINT_ID";
        private static final String KEY_VALUE_DELIMITER = "=";
        private static final String VALUES_DELIMITER = ";";
        private static final String META_BLOCK_START = "%META_START%";
        private static final String META_BLOCK_END = "%META_END%";

        /**
         * Базовый конструктор билдера, который инициализирует объект
         *
         * @param context контекст приложения (необходим для связывания файлового менеджера)
         */
        public Builder(Context context) {
            applicationFileManager = ApplicationFileManager.getInstance(context);
        }

        /**
         * Добавление списка с метками времени для дальнейшей их обработки
         *
         * @param timePoints список объектов "Метки времени"
         * @return инстанс билдера (для реализации chaining - построение цепочки вызова методов)
         */
        public Builder addTimepoints(ArrayList<TimePoint> timePoints) {
            this.timePoints.addAll(timePoints);
            return this;
        }

        /**
         * Добавление информации по названию события
         *
         * @param eventName название события
         * @return инстанс билдера (для реализации chaining - построение цепочки вызова методов)
         */
        public Builder addEventName(String eventName) {
            addMetaElement(META_KEY_EVENT_NAME, eventName);
            return this;
        }

        /**
         * Добавление информации о количестве кругов
         *
         * @param lapsCount количество кругов в событии
         * @return инстанс билдера (для реализации chaining - построение цепочки вызова методов)
         */
        public Builder addLapsCount(int lapsCount) {
            addMetaElement(META_KEY_LAPS_COUNT, String.valueOf(lapsCount));
            return this;
        }

        /**
         * Добавление информации о количестве контрольных точек
         *
         * @param checkPointsCount количество контрольных точек
         * @return инстанс билдера (для реализации chaining - построение цепочки вызова методов)
         */
        public Builder addCheckPointsCount(int checkPointsCount) {
            addMetaElement(META_KEY_CHECKPOINTS_COUNT, String.valueOf(checkPointsCount));
            return this;
        }

        /**
         * Добавление идентификатора контрольной точки, для которой формируется протокол
         *
         * @param pointId идентификатор контрольной точки
         * @return инстанс билдера (для реализации chaining - построение цепочки вызова методов)
         */
        public Builder addPointId(int pointId) {
            addMetaElement(META_KEY_POINT_ID, String.valueOf(pointId));
            return this;
        }

        /**
         * Добавление мета-элемента в файл протокола (служебная информация для обработки протокола)
         *
         * @param metaKey   ключ мета-элемента
         * @param metaValue значения мета-элемента
         */
        private void addMetaElement(String metaKey, String metaValue) {
            metaElements.add(metaKey + KEY_VALUE_DELIMITER + metaValue);
        }

        /**
         * Запись блока мета-информации в файл протокола на основе добавленных мета-элементов
         */
        private void writeMeta() {
            dataBuilder.append(META_BLOCK_START);
            for (String meta : metaElements) {
                dataBuilder.append('\n').append(meta);
            }
            dataBuilder.append('\n').append(META_BLOCK_END);
        }

        /**
         * Получение информации по участникам для протокола
         *
         * @return набор пар {номер участника -> список времён контрольных точек участника}
         */
        private HashMap<Integer, ArrayList<Long>> getParticipants() {
            HashMap<Integer, ArrayList<Long>> resultMap = new HashMap<>();
            for (TimePoint timePoint : timePoints) {
                /* Извлечение диапазона участников и времени в исходном формате из объекта "Метка времени" */
                String timePointParticipantValue = timePoint.getParticipant();
                long time = timePoint.getRawTime();

                /* Инициализация множества участников, к которому привязана метка времени */
                HashSet<Integer> participantSet = new HashSet<>();
                /* Диапазон бьётся на под-диапазоны, разделенные запятыми */
                String[] participantRanges = timePointParticipantValue.split(",");
                for (String participantRange : participantRanges) {
                    /* В под-диапазоне определяется наличие дефиса */
                    int dashIndex = participantRange.indexOf('-');
                    if (dashIndex >= 0) {
                        /*
                         * Если дефис присутствует, то под-диапазон содержит последовательный список номеров участников
                         * Задаются начальный и конечный номер под-диапазона
                         */
                        int startRange = Integer.parseInt(participantRange.substring(0, dashIndex));
                        int endRange = Integer.parseInt(participantRange.substring(dashIndex + 1));
                        if (startRange > endRange) {
                            /*
                             * Если начальный номер больше конечного, то "разворачиваем" под-диапазон
                             * Для этого меняем местами значения startRange и endRange с помощью операции "Исключающее ИЛИ"
                             */
                            startRange ^= endRange; // start = start XOR end
                            endRange ^= startRange; // end = end XOR (end XOR start) = start
                            startRange ^= endRange; // start = start XOR (end XOR start) = end
                        }
                        /* Заполняем множество участников последовательным списком из под-диапазона */
                        for (int i = startRange; i <= endRange; ++i) {
                            participantSet.add(i);
                        }
                    } else {
                        /* Если дефис отсутствует, то под-диапазон представлен одним номером участника */
                        participantSet.add(Integer.parseInt(participantRange));
                    }
                }

                /*
                 * На основе диапазона сформировано множество участников, к которому привязана данная метка времени
                 * Для каждого участника из множества добавляем время в его список меток времени
                 */
                for (int participant : participantSet) {
                    if (resultMap.get(participant) == null) {
                        resultMap.put(participant, new ArrayList<>());
                    }
                    Objects.requireNonNull(resultMap.get(participant)).add(time);
                }
            }
            return resultMap;
        }

        /**
         * Запись информации об участниках в итоговый протокол
         */
        private void writeParticipants() {
            /* Получение информации об участниках в виде набора пар {номер участника -> список времён контрольных точек участника} */
            HashMap<Integer, ArrayList<Long>> participants = getParticipants();

            /* Запись каждой пары в протокол */
            for (HashMap.Entry<Integer, ArrayList<Long>> participantEntry : participants.entrySet()) {
                /* Список меток времени участника сортируется по возрастанию */
                ArrayList<Long> participantTimes = participantEntry.getValue();
                Collections.sort(participantTimes);

                /* В протокол добавляется строка с ключом, который представляет собой номер участника */
                dataBuilder.append('\n').append(participantEntry.getKey()).append(KEY_VALUE_DELIMITER);
                /* В протокол добавляются времена в исходном формате (UNIX-формат) для данного ключа */
                for (long participantTime : participantTimes) {
                    dataBuilder.append(participantTime).append(VALUES_DELIMITER);
                }
            }
        }

        /**
         * Создание объекта "Протокол", который содержит информацию по событию и информацию по участникам и их меткам времени
         *
         * @return объект "Протокол" с информацией в строковом формате, которую потом можно записать в файл протокола
         * @see #writeFile(String)  запись файла протокола
         */
        public Protocol create() {
            addMetaElement(META_KEY_TIME_PATTERN, DateTimeFormatter.getTimePattern());
            addMetaElement(META_KEY_TIME_ZONE, DateTimeFormatter.getTimeZone());
            writeMeta();
            writeParticipants();
            return new Protocol(dataBuilder.toString());
        }
    }
}
