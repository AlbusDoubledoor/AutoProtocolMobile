package rekov.graduate.autoprotocol.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.activities.viewfiles.ViewProtocolsActivity;
import rekov.graduate.autoprotocol.adapters.TimePointsAdapter;
import rekov.graduate.autoprotocol.configurations.EventConfiguration;
import rekov.graduate.autoprotocol.configurations.PointConfiguration;
import rekov.graduate.autoprotocol.timepoint.TimePoint;
import rekov.graduate.autoprotocol.protocol.Protocol;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;
import rekov.graduate.autoprotocol.utils.DateTimeFormatter;
import rekov.graduate.autoprotocol.utils.FileUtils;

/**
 * Активность "Обработка события"
 * Задача: создание меток времени события и формирование файла протокола
 */
public class ProcessEventActivity extends BaseActivity implements View.OnClickListener {
    private static final int CHRONO_UPDATE_RATE = 1;
    private static final int BUTTON_ID__FIX_TIME = R.id.BTN__FIX_TIME;
    private static final int BUTTON_ID__STOP_TIME = R.id.BTN__STOP_TIME;
    private static final int BUTTON_ID__ADD_TIME_POINT = R.id.BTN__ADD_TIME_POINT;
    private static final int BUTTON_ID__FINISH_EVENT = R.id.BTN__FINISH_EVENT;
    private static final String DEFAULT_PROTOCOL_FILE_NAME = "protocol";
    private long eventBaseTime;
    private Protocol protocol;
    private TextView chrono;
    private Timer mainTimer;
    private TimePointsAdapter timePointsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_event);

        /* Скрываем ActionBar */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        /* Получаем точку отсчета, заданную при синхронизации (передаётся из активности Синхронизация) */
        eventBaseTime = getIntent().getLongExtra(INTENT_EXTRA_KEY_BASE_TIME, System.currentTimeMillis());

        timePointsAdapter = new TimePointsAdapter(this);

        /*
         *  Устанавливаем максимальный номер участника из Конфигурации события
         *  Так как на прогрузку Конфигурации события требуется время - задача выполняется в отдельном потоке
         */
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> timePointsAdapter.setMaxParticipant(Objects.requireNonNull(EventConfiguration.getCurrent(this)).getMaxParticipant()));

        /* В основным RecyclerView устанавливаем адаптер меток времени (список) */
        RecyclerView recyclerView = findViewById(R.id.LIST__TIME_POINTS);
        recyclerView.setAdapter(timePointsAdapter);

        chrono = findViewById(R.id.TXT__VIEW_TIME);

        findViewById(BUTTON_ID__FIX_TIME).setOnClickListener(this);
        findViewById(BUTTON_ID__STOP_TIME).setOnClickListener(this);
        findViewById(BUTTON_ID__ADD_TIME_POINT).setOnClickListener(this);
        findViewById(BUTTON_ID__FINISH_EVENT).setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mainTimer == null) {
            /*
             *  При старте активности, если таймер ещё неинициализирован, то инициализируем
             *  Ставим задачу обновления секундомера каждую миллисекунду
             *  Добавляем начальную пустую метку времени в адаптер
             */
            mainTimer = new Timer();
            mainTimer.scheduleAtFixedRate(new UpdateChrono(), 0, CHRONO_UPDATE_RATE);
            timePointsAdapter.addTimePoint(new TimePoint());
        }
    }

    /**
     * Задача обновления секундомера (обновление происходит с фиксированным интервалом, 1 миллисекунда)
     * Значение секундомера = текущее время - точка отсчёта события
     */
    private class UpdateChrono extends TimerTask {
        public void run() {
            chrono.setText(DateTimeFormatter.formatTime(System.currentTimeMillis() - eventBaseTime));
        }
    }

    /**
     * Переопределение поведения кнопки "Назад"
     * Вызывается диалоговое окно с подтверждением выхода из события
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dlg_msg__exit_event)
                .setPositiveButton(R.string.dlg_btn__exit, (d, id) -> super.onBackPressed())
                .setNegativeButton(R.string.dlg_btn__abort, (d, id) -> {
                });
        builder.create().show();
    }

    /**
     * При уничтожении события гарантируем остановку таймера и уничтожение объектов адаптера
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainTimer != null) {
            mainTimer.cancel();
        }
        if (timePointsAdapter != null) {
            timePointsAdapter.destroy();
        }
    }

    /**
     * Обработчик нажатия на кнопки активности
     *
     * @param buttonView кнопка, по которой нажали
     */
    @Override
    public void onClick(View buttonView) {
        /* Если текущий фокус на редактируемом поле, то фокус нужно сбросить (скрыть клавиатуру, утвердить значение поля) */
        if (getCurrentFocus() instanceof EditText) {
            getCurrentFocus().clearFocus();
        }
        switch (buttonView.getId()) {
            case BUTTON_ID__FIX_TIME:
                /* По нажатию на кнопку "Время" - в адаптере обновляется пустая метка времени */
                timePointsAdapter.updateTime(System.currentTimeMillis() - eventBaseTime);
                break;
            case BUTTON_ID__STOP_TIME:
                /*
                 *  По нажатию на кнопку "Остановить время" (красная иконка):
                 *  1) Вызывается диалоговое окно с подтверждением остановки времени
                 *  2) Если остановка подтверждена, то происходит проверка на присутствие "неготовых" меток времени
                 *  3) Если "неготовые" метки времени присутствуют, то выводится диалоговое окно с уведомлением
                 *  4) Если все метки времени "готовы", то проивзодится Остановка времени
                 */
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dlg_msg__finish_event);
                builder.setNegativeButton(R.string.dlg_btn__abort, (dialog, which) -> {
                });
                builder.setPositiveButton(R.string.dlg_btn__finish, (dialog, which) -> {
                    if (timePointsAdapter.hasUnready()) {
                        AlertDialog.Builder subDialogBuilder = new AlertDialog.Builder(this);
                        subDialogBuilder
                                .setMessage(R.string.dlg_msg__unready_event)
                                .setPositiveButton(R.string.dlg_btn__close, (subDialog, subWhich) -> {
                                });
                        dialog.dismiss();
                        subDialogBuilder.create().show();
                    } else {
                        /*
                         * Остановка времени
                         * 1) Адаптер меток времени переходит в режим "ревью" (просмотр)
                         * 2) Кнопки "Добавить метку", "Время", "Остановить время" - скрываются с формы
                         * 3) Появляется кнопка "Завершить событие" (Продолжить)
                         */
                        timePointsAdapter.showReview();

                        mainTimer.cancel();

                        findViewById(R.id.BTN__ADD_TIME_POINT).setVisibility(View.GONE);
                        findViewById(R.id.BTN__FIX_TIME).setVisibility(View.GONE);
                        buttonView.setVisibility(View.GONE);

                        findViewById(R.id.BTN__FINISH_EVENT).setVisibility(View.VISIBLE);
                    }
                });
                builder.create().show();
                break;
            case BUTTON_ID__ADD_TIME_POINT:
                /* Кнопка "Добавить метку времени" добавляет пустую метку времени в список */
                timePointsAdapter.addTimePoint(new TimePoint());
                break;

            case BUTTON_ID__FINISH_EVENT:
                /*
                 *  Кнопка "Завершить событие" (продолжить):
                 *  1) Происходит построение протокола (с использованием конфигураций события и точки)
                 *  2) Выводится диалоговое окно с предложением выбрать имя файла
                 *  3) Производится сохранение файла протокола
                 */
                if (protocol == null) {
                    Protocol.Builder protocolBuilder = new Protocol.Builder(this);
                    EventConfiguration eventConfiguration = EventConfiguration.getCurrent(this);
                    PointConfiguration pointConfiguration = PointConfiguration.get(this);
                    if (eventConfiguration != null) {
                        protocolBuilder
                                .addEventName(eventConfiguration.getEventName())
                                .addLapsCount(eventConfiguration.getLapsCount())
                                .addCheckPointsCount(eventConfiguration.getCheckPointsCount());
                    }
                    if (pointConfiguration != null) {
                        protocolBuilder
                                .addPointId(pointConfiguration.getPointId());
                    }
                    protocol = protocolBuilder.addTimepoints(timePointsAdapter.getTimePoints()).create();
                }
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder
                        .setTitle(R.string.dlg_title__choose_protocol_file_name)
                        .setView(R.layout.layout_file_name)
                        .setPositiveButton(R.string.dlg_btn__save, (dialog, which) -> {
                        });
                AlertDialog dialog = dialogBuilder.create();
                dialog.show();
                String protocolFileNamePlaceHolder = DEFAULT_PROTOCOL_FILE_NAME + "_" + DateTimeFormatter.formatDate(System.currentTimeMillis());
                EditText protocolFileNameEdit = dialog.findViewById(R.id.file_name);
                Objects.requireNonNull(protocolFileNameEdit).setHint(protocolFileNamePlaceHolder);
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener((positiveView) -> {
                    String protocolFileName = protocolFileNameEdit.getText().toString();
                    if (protocolFileName.equals("")) {
                        protocolFileName = protocolFileNameEdit.getHint().toString();
                    }
                    protocolFileName = FileUtils.escape(protocolFileName);
                    if (protocol.writeFile(protocolFileName)) {
                        /*
                         *  Если файл протокола успешно сохранён:
                         *  1) Уничтожается диалоговое окно
                         *  2) Удаляются "объекты" - Конфигурация события и Конфигурация точки
                         *  3) Запускается активность "Просмотр файлов протоколов"
                         */
                        dialog.dismiss();
                        ApplicationFileManager.getInstance(this).deleteObjects();
                        finish();
                        startActivity(new Intent(this, ViewProtocolsActivity.class));
                    } else {
                        /* Если файл протокола не удалось сохранить, то выводится диалоговое окно с уведомлением */
                        AlertDialog.Builder operationFailedNotify = new AlertDialog.Builder(this);
                        operationFailedNotify.setMessage(R.string.dlg_msg__protocol_write_fail);
                        operationFailedNotify.setPositiveButton(R.string.dlg_btn__close, (subDialog, subWhich) -> {
                        });
                        dialog.dismiss();
                        operationFailedNotify.create().show();
                    }
                });
                break;
        }
    }
}