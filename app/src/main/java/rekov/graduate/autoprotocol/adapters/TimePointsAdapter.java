package rekov.graduate.autoprotocol.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.timepoint.TimePoint;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Расширение адаптера элемента RecyclerView для отображения меток времени события (Time Point)
 */
public class TimePointsAdapter extends RecyclerView.Adapter<TimePointsAdapter.ViewHolder> implements View.OnFocusChangeListener, View.OnClickListener {
    private static final int VIEW_HOLDER_OBJECT_TAG = R.id.VIEW_TAG__VIEW_HOLDER;
    private static final String timePointsTmpDir = "timepoints";
    private final LayoutInflater inflater;
    private final Context context;
    private final ArrayList<TimePoint> timePoints;
    private final ArrayList<TimePoint> saveFailedTimePoints;
    private final ExecutorService fileOpsExecutor;
    private final ApplicationFileManager applicationFileManager;
    private int maxParticipant = -1;
    private boolean isReviewMode = false;
    private int hiddenTimepoints = 0;

    /**
     * Базовый конструктор адаптера для иницилизации списка
     * Контекст приложения необходим для связывания файлового менеджера, получения LayoutInflater, ...
     * Конструктор инициализирует необходимые списки, связывает ресурсы и порождает Executor для выполнения операций с файлами в фоне
     *
     * @param context контекст приложения
     */
    public TimePointsAdapter(Context context) {
        fileOpsExecutor = Executors.newSingleThreadExecutor();
        saveFailedTimePoints = new ArrayList<>();
        timePoints = new ArrayList<>();
        this.context = context;
        inflater = LayoutInflater.from(context);
        applicationFileManager = ApplicationFileManager.getInstance(context);
    }

    /**
     * Освобождение ресурсов, занимаемого адаптером
     * 1) Завершается поток Executor'а, который выполнял операции с файлами в фоне
     * 2) Очищается основной список меток времени
     * 3) Очищается список с сохраненными метками времени в ОЗУ (которые не получилось записать на память устройства)
     * 4) Очищается временная директория с сохраненными метками времени (на памяти устройства)
     */
    public void destroy() {
        fileOpsExecutor.shutdown();
        timePoints.clear();
        saveFailedTimePoints.clear();
        applicationFileManager.clearTempDir(timePointsTmpDir);
    }

    /**
     * Сохранение метки времени на памяти устройства или в ОЗУ
     * Скрытие меток необходимо для освобождения ресурсов оперативной памяти и для освобождения места на экранной форме
     * На память устройства метка времени сохраняется в сериализованном виде во временной директории в виде текстового файла
     *
     * @param timePoint объект метки времени, которую нужно сохранить
     */
    private void saveTimepoint(TimePoint timePoint) {
        String fileName = java.util.UUID.randomUUID().toString().toLowerCase().replace("-", "");
        if (!applicationFileManager.writeTempFile(timePointsTmpDir, fileName, TimePoint.serialize(timePoint))) {
            /* Если не удалось сохранить точку на память устройства, то запоминаем в ОЗУ */
            saveFailedTimePoints.add(timePoint);
        }
        /* Для корректной валидации общего списка меток требуется подсчитывать количество "скрытых" меток */
        ++hiddenTimepoints;
    }

    /**
     * Извлечение всех сохраненных меток времени
     * Итоговый список содержит все скрытые метки из временной директории (метки времени десериализуются из текстовых файлов)
     * А также итоговый список содержит все скрытые метки, сохраненные в ОЗУ (неудачные сохранения во временной директории)
     * После извлечения меток, временная директория очищается в фоне Executor'ом, а список с сохраннеными метками очищается (для освобождения ресурсов ОЗУ)
     *
     * @return итоговый список всех сохраненных в ОЗУ и во временной директории на устройстве "скрытых" меток времени
     */
    private ArrayList<TimePoint> getSavedTimePoints() {
        ArrayList<TimePoint> result = new ArrayList<>();
        /*
         * Временная директория с метками считывается полностью в список строк
         * Каждая строка должна представлять собой сериализованную метку времени (TimePoint.serialize)
         */
        for (String timePointSerialized : applicationFileManager.readTempDir(timePointsTmpDir)) {
            result.add(TimePoint.deserialize(timePointSerialized));
        }
        /* Очистка временной директории может быть выполнена в фоне */
        fileOpsExecutor.execute(() -> ApplicationFileManager.getInstance(context).clearTempDir(timePointsTmpDir));

        /* Если были метки времени, которые не удалось записать на память устройства, то скрытые с формы метки записывались в отдельный список */
        result.addAll(saveFailedTimePoints);
        saveFailedTimePoints.clear();
        return result;
    }

    /**
     * Проверка адаптера на присутствие "неготовых" меток времени
     * Метка готова, если у неё заполнены время и диапазон участников
     * Если в адаптере не содержится ни одной метки и ни одной метки не было скрыто с формы, то это также делает адаптер "неготовым"
     *
     * @return флаг готовности адаптера (хотя бы одна метка была готова и отсутствуют "неготовые" метки)
     */
    public boolean hasUnready() {
        /* Если не было ни одной готовой метки (ни в адаптере, ни в скрытых), то это тоже делает адаптер "неготовым" */
        if (timePoints.size() + hiddenTimepoints == 0) {
            return true;
        }

        /* Каждая метка времени в адаптере проверяется на "готовность" */
        for (TimePoint tp : timePoints) {
            if (!tp.isReady()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Перевод адаптера в режим "ревью" (просмотра)
     * В текущий список меток времени добавляются сохраненные прежде (скрытые с формы)
     * Все метки времени сортируются по возрастанию времени
     * Проставляется флаг режима "ревью"
     * Адаптер перестраивается на основе новых данных
     */
    public void showReview() {
        timePoints.addAll(getSavedTimePoints());
        Collections.sort(timePoints);

        isReviewMode = true;
        notifyDataSetChanged();
    }

    /**
     * Установка "максимального" номера участника
     * Данный параметр применяется при форматировании диапазона
     *
     * @param maxParticipant номер верхней границы диапазона участников
     */
    public void setMaxParticipant(int maxParticipant) {
        this.maxParticipant = maxParticipant;
    }

    /**
     * Асинхронное добавление метки времени в адаптер
     *
     * @param tp добавляемый объект метки времени
     */
    public void addTimePoint(TimePoint tp) {
        timePoints.add(tp);
        notifyItemInserted(timePoints.size() - 1);
    }

    /**
     * @return оригинальный список с метками времени
     */
    public ArrayList<TimePoint> getTimePoints() {
        return timePoints;
    }

    /**
     * Обновление первой пустой метки времени
     * Если пустых меток времени нет в адаптере, то добавляется новая метка времени
     *
     * @param rawTime время в исходном формате (UNIX-формат)
     */
    public void updateTime(long rawTime) {
        int firstEmptyTimePointPos = -1;

        /* Поиск в списке меток первой пустой */
        int tpSize = timePoints.size();
        for (int currentTimePointIdx = 0; currentTimePointIdx < tpSize; ++currentTimePointIdx) {
            if (timePoints.get(currentTimePointIdx).isEmpty()) {
                firstEmptyTimePointPos = currentTimePointIdx;
                break;
            }
        }

        if (firstEmptyTimePointPos >= 0) {
            /* Если пустая метка времени найдена, то обновляется её значение */
            timePoints.get(firstEmptyTimePointPos).setTime(rawTime);
            notifyItemChanged(firstEmptyTimePointPos);
        } else {
            /* Иначе в адаптер добавляется новая метка времени без диапазона участников */
            addTimePoint(new TimePoint(rawTime));
        }
    }

    /**
     * Создание ViewHolder из заданного Layout для элемента списка с метками времени
     *
     * @param parent   родитель списка (RecyclerView)
     * @param viewType тип view (не применяется)
     * @return ViewHolder созданного элемента списка
     */
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.time_point_list_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Конфигурация элемента списка после привязки ViewHolder
     *
     * @param holder   привязанный ViewHolder
     * @param position позиция в адаптере для элемента списка, к которому привязан ViewHolder
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TimePoint tp = timePoints.get(position);

        /* В поле "Время" заносится форматированное значение метки времени */
        holder.timeView.setText(tp.getTime());

        /* В поле "Участник" заносится диапазон участников, к которому привязана данная метка времени */
        holder.participantView.setText(tp.getParticipant());
        if (isReviewMode) {
            /* В режиме "Ревью" кнопка "Скрыть" удаляется с формы, а поле "Участник" отключается */
            holder.participantView.setEnabled(false);
            holder.buttonHide.setVisibility(View.GONE);
        } else {
            /*
             * Если режим не "Ревью", то к кнопке "Скрыть" и полю "Участник" добавляются тэг-объекты с их ViewHolder'ами
             * А также устанавливаются "слушатели" (listeners) событий (смена фокуса и нажатие по кнопке)
             */
            holder.participantView.setTag(VIEW_HOLDER_OBJECT_TAG, holder);
            holder.participantView.setOnFocusChangeListener(this);

            holder.buttonHide.setTag(VIEW_HOLDER_OBJECT_TAG, holder);
            holder.buttonHide.setOnClickListener(this);
        }
    }

    @Override
    public int getItemCount() {
        return timePoints.size();
    }

    /**
     * Расширение RecyclerView.ViewHolder для адаптера меток времени
     * Содержит следующие элементы:
     * 1) Текстовое поле для отображения времени в строковом формате
     * 2) Редактируемое текстовое поле для диапазона участников, к которому привязана метка
     * 3) Кнопку "Скрыть", которая удаляет элемент с формы и сохраняет на устройстве (или в неотображаемом другом списке в ОЗУ)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements TextView.OnEditorActionListener {
        final TextView timeView;
        final EditText participantView;
        final Button buttonHide;

        ViewHolder(View view) {
            super(view);
            timeView = view.findViewById(R.id.time);

            /* Для редактируемого поля с диапазоном участников устанавливается числовая клавиатура и "слушатель" действий редактора */
            participantView = view.findViewById(R.id.time_owner);
            participantView.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            participantView.setOnEditorActionListener(this);

            buttonHide = view.findViewById(R.id.btn_hide);
        }

        /**
         * Обработчик действий редактора (для редактируемого поля с диапазаном участников)
         * По действию IME "Done" (завершение редактирования на клавиатуре), нужно сбросить фокус с поля
         *
         * @param view     поле, к которому привязан Editor (редактор) - диапазон участников
         * @param actionId специальный идентификатор действия (коды IME-кнопок и т.п.)
         * @param event    событие клавиши на клавиатуре (в нашем случае не требуется)
         * @return всегда false (таков интерфейс для завершенного действия)
         */
        @Override
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                view.clearFocus();
            }
            return false;
        }
    }

    /**
     * Обработчик события смены фокуса на редактируемом поле диапазона участников
     * После утраты фокуса, нужно произвести обработку введенного текста и скрыть клавиатуру
     *
     * @param view     поле, на котором произошла смена фокуса (в нашем случае - диапазон участников)
     * @param hasFocus флаг фокуса на элементе (в случае приобретения - true, в случае утраты - false)
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (!hasFocus) {
            String text = ((EditText) view).getText().toString();
            /*
             * Обработка введенного текста на основе регулярных выражений:
             * 1) Пробелы и дефисы между двумя диапазонами заменяются на запятую
             * 2) Любые символы, кроме цифр, запятой и дефиса, - удаляются
             * 3) Дублированные запятые и дефисы удаляются (остаётся только один символ)
             * 4) Лидирующие нули в числах (в том числе только 0) - удаляются
             * 5) Бессмысленные запятые и дефисы в начале строки и в конце - удаляются
             */
            text = text.replaceAll("([\\d\\-])\\s+([\\-\\d])", "$1,$2");
            text = text.replaceAll("[^\\d,\\-]", "");
            text = text.replaceAll("([,\\-]){2,}", "$1");
            text = text.replaceAll("(([^\\d])|(^))0+", "$1");
            text = text.replaceAll("(([^\\d]|^)[\\-,])|([\\-,]([^\\d]|$))", "");

            /*
             * Если задана верхняя граница диапазона участников и обработанный текст содержит диапазон (не пустой):
             * 1) Создается массив номеров участников
             * 2) Проверяется каждый введенный номер участника
             * 3) Если номер превышает границу, то он заменяется на максимальный номер
             */
            if (maxParticipant >= 0 && !text.equals("")) {
                String[] participants = text.split("[,-]");
                for (String participantNumber : participants) {
                    if (Integer.parseInt(participantNumber) > maxParticipant) {
                        text = text.replace(participantNumber, String.valueOf(maxParticipant));
                    }
                }
            }

            /* Клавиатуру нужно скрыть, иначе возможны баги и неконтролируемое поведение формы */
            InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);

            /* Уведомляем адаптер, что метка времени была изменена (позицию в адаптере получаем с помощью тэг-объекта ViewHolder */
            int adapterPosition = ((ViewHolder) view.getTag(VIEW_HOLDER_OBJECT_TAG)).getAdapterPosition();
            timePoints.get(adapterPosition).setParticipant(text);
            notifyItemChanged(adapterPosition);
        }
    }

    /**
     * Обработчик клика по кнопке "Скрыть"
     * После нажатия нужно:
     * 1) Сбросить фокус с редактируемых полей
     * 2) Проверить "готовность" метки времени
     * Если метка времени готова, то:
     * 2.1) Сохранить метку времени на памяти устройства или, в случае неудачи, в отдельном неотображаемом списке (т.е. в ОЗУ)
     * 2.2) Удалить элемент из адаптера (скрыть с формы)
     * Иначе
     * 3) Вывести диалоговое окно с уведомлением о неготовности метки и предложением удалить метку времени
     *
     * @param buttonView кнопка, по которой совершено нажатие (в нашем случае - кнопка "Скрыть")
     */
    @Override
    public void onClick(View buttonView) {
        /*
         * Требуется сбросить фокус с редактируемого поля
         * Получение текущего фокуса в активности может породить исключение (контекст может быть представлен другим классом)
         */
        try {
            View currentFocusView = ((Activity) context).getCurrentFocus();
            if (currentFocusView instanceof EditText) {
                currentFocusView.clearFocus();
            }
        } catch (Exception ignored) {
            // Исключение игнорируется (если текущий фокус не удалось получить, то фокус был не на редактируемом поле)
        }

        /* Получаем текущую позицию элемента в адаптере с помощью тэг-объекта ViewHolder */
        int adapterPosition = ((ViewHolder) buttonView.getTag(VIEW_HOLDER_OBJECT_TAG)).getAdapterPosition();
        if (timePoints.get(adapterPosition).isReady()) {
            /* Если текущая метка времени "готова", то производим сохранение метки и удаляем из адаптера (скрываем с формы) */
            TimePoint tpToSave = new TimePoint(timePoints.get(adapterPosition));
            /* Сохранение метки в памятиу устройства или в отдельном списке в ОЗУ можно выполнить в фоне */
            fileOpsExecutor.execute(() -> saveTimepoint(tpToSave));
            timePoints.remove(adapterPosition);
            notifyItemRemoved(adapterPosition);
        } else {
            /* Если текущая метка времени "не готова", то выводим диалоговое окно с уведомлением */
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.dlg_msg__hide_time_point);
            builder.setPositiveButton(R.string.dlg_btn__close, (dialog, which) -> {
                // По нажатию на кнопку "Закрыть", диалоговое окно просто уничтожается
            });
            builder.setNeutralButton(R.string.dlg_btn__delete, (dialog, which) -> {
                /* По нажатию на кнопку "Удалить", метка времени удаляется из адаптера */
                timePoints.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
            });
            builder.create().show();
        }
    }
}
