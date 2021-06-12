package rekov.graduate.autoprotocol.configurations;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Объект "Конфигурация точки"
 * Используется для записи информации по контрольной точке в итоговый протокол
 */
public class PointConfiguration implements Serializable {
    private static final String CURRENT_CONFIG_NAME = "pointconf";
    private static ApplicationFileManager applicationFileManager;
    private int pointId = 0;

    /**
     * Базовый конструктор для инициализации полей
     *
     * @param context контекст приложения (необходим для связывание файлового менеджера приложения)
     */
    public PointConfiguration(Context context) {
        applicationFileManager = ApplicationFileManager.getInstance(context);
    }

    public void setPointId(int pointId) {
        this.pointId = pointId;
    }

    public int getPointId() {
        return pointId;
    }

    /**
     * Применение Конфигурации точки
     * (по сути запись объекта в специальный файл)
     *
     * @param pointConfiguration объект "Конфигурация точки", который будет использован для применения конфигурации
     * @return флаг успешной операции применения
     * @see #get(Context) получение примененной конфигурации
     */
    public static boolean apply(@NonNull PointConfiguration pointConfiguration) {
        return applicationFileManager.writeObjectFile(CURRENT_CONFIG_NAME, pointConfiguration);
    }

    /**
     * Получение текущей Конфигурации точки
     * (по сути чтение объекта из специального файла)
     *
     * @param context контекст приложения (необходим для связывания файлового менеджера приложения)
     * @return текущая Конифгурация точки (может быть null, если конфигурации нет)
     * @see #apply(PointConfiguration) метод, с помощью которого применяется текущая конфигурация
     */
    @Nullable
    public static PointConfiguration get(Context context) {
        applicationFileManager = ApplicationFileManager.getInstance(context);
        return (PointConfiguration) applicationFileManager.readObjectFile(CURRENT_CONFIG_NAME);
    }
}
