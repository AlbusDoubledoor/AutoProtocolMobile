package rekov.graduate.autoprotocol.timepoint;

import rekov.graduate.autoprotocol.utils.DateTimeFormatter;

/**
 * Объекты "Метка времени", содержащие данные по событию
 */
public class TimePoint implements Comparable<TimePoint> {
    private static final String EMPTY_PLACEHOLDER = "---";
    private static final String SERIALIZE_DELIMITER = "%";
    private long rawTime = 0;
    private String time = "";
    private String participant = "";
    private boolean isEmpty = true;
    private transient boolean isReady = false;

    /**
     * Конструктор пустой метки времени
     * Метка пустая и не готова
     * Строка времени заменяется плейсхолдером
     */
    public TimePoint() {
        time = EMPTY_PLACEHOLDER;
    }

    /**
     * Конструктор копирования метки времени
     *
     * @param timePoint источник копирования
     */
    public TimePoint(TimePoint timePoint) {
        setTime(timePoint.rawTime);
        participant = timePoint.participant;
    }

    /**
     * Установка флага готовности метки времени
     */
    private void setReady() {
        isReady = participant != null && !participant.equals("") && !isEmpty;
    }


    /**
     * Конструктор метки времени с заполненным временем
     *
     * @param rawTime время метки в UNIX-формате
     */
    public TimePoint(long rawTime) {
        setTime(rawTime);
    }

    /**
     * Конструктор метки времени со всеми заполненными параметрами
     *
     * @param rawTime     время метки в UNIX-формате
     * @param participant диапазон участников, к которым привязана метка
     */
    public TimePoint(long rawTime, String participant) {
        isEmpty = false;
        this.rawTime = rawTime;
        time = DateTimeFormatter.formatTime(rawTime);
        this.participant = participant;
        setReady();
    }

    /**
     * @return флаг пустоты метки
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * @return флаг готовности метки
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * Обновление времени метки
     *
     * @param rawTime время в UNIX-формате
     */
    public void setTime(long rawTime) {
        this.rawTime = rawTime;
        time = DateTimeFormatter.formatTime(rawTime);
        isEmpty = false;
        setReady();
    }

    /**
     * Обновление диапазона участников, к которым привязана метка
     *
     * @param participant диапазон участников (могут быть разделены запятыми или с помощью дефиса)
     */
    public void setParticipant(String participant) {
        this.participant = participant;
        setReady();
    }

    /**
     * Десериализация метки времени из строки
     *
     * @param serialized метка времени в виде строки (в определенном формате)
     * @return объект метки времени
     * @see #serialize(TimePoint) метод сериализации метки времени
     */
    public static TimePoint deserialize(String serialized) {
        String[] split = serialized.split(SERIALIZE_DELIMITER);
        return new TimePoint(Long.parseLong(split[1]), split[0]);
    }

    /**
     * Сериализация метки времени в строку
     *
     * @param timePoint метка времени, которую нужно сериализовать
     * @return метка времени в виде строки (в определенном формате)
     * @see #deserialize(String) метод десериализации метки времени
     */
    public static String serialize(TimePoint timePoint) {
        return timePoint.participant + SERIALIZE_DELIMITER + timePoint.rawTime;
    }

    /**
     * @return время метки (в определенном формате)
     * @see DateTimeFormatter класс, применяемый для форматирования времени
     */
    public String getTime() {
        return time;
    }

    /**
     * @return время метки в исходном формате (UNIX-формат)
     */
    public long getRawTime() {
        return rawTime;
    }

    /**
     * @return диапазон участников, к которым привязана метка (могут быть разделены запятыми или с помощью дефиса)
     */
    public String getParticipant() {
        return participant;
    }

    /**
     * Компаратор меток времени
     * Сравниваются времена метки в исходном формате (UNIX-формат)
     *
     * @param other другая метка времени
     * @return -1 (если текущая метка была раньше), 0 (если метки равны), 1 (если текущая метка была позже)
     */
    @Override
    public int compareTo(TimePoint other) {
        return Long.compare(rawTime, other.rawTime);
    }
}