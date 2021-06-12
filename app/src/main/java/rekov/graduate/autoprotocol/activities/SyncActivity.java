package rekov.graduate.autoprotocol.activities;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.EventLog;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.configurations.EventConfiguration;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Активность "Синхронизация"
 * Задача: инициализировать событие и задать точку отсчета секундомера
 */
public class SyncActivity extends BaseActivity implements View.OnClickListener {
    private static final int buttonSyncId = R.id.BTN__SYNC;
    private static final int buttonStartEventId = R.id.BTN__START_EVENT;
    private static final int syncProgressbarId = R.id.PRBAR__SYNC;
    private static final int chkAutoSyncId = R.id.CHK__AUTO_SYNC;
    private long eventBaseTime;
    private Timer syncTimer;
    private TextView txtSyncHint;
    private Button buttonStartEvent;
    private Button buttonSync;
    private ProgressBar syncProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        setActionBarTitle(R.string.title_activity_sync);

        syncTimer = new Timer();

        buttonSync = findViewById(buttonSyncId);
        buttonStartEvent = findViewById(buttonStartEventId);

        buttonStartEvent.setOnClickListener(this);
        buttonSync.setOnClickListener(this);

        syncProgressBar = findViewById(syncProgressbarId);
        txtSyncHint = findViewById(R.id.TXT__SYNC_HINT);
    }

    /**
     * Задача завершения синхронизации
     * 1) Все действия передаются в поток, обрабатывающий UI
     * 2) Удаляется троббер синхронизации
     * 3) Анимированно отображается изображение, символизирующее завершение синхронизации
     * 4) Проигрывается рингтон уведомления
     * 5) Появляется кнопка "Продолжить"
     */
    class FinishSync extends TimerTask {
        public void run() {
            Handler uiThreadHandler = buttonStartEvent.getHandler();
            uiThreadHandler.post(() -> {
                txtSyncHint.setVisibility(View.GONE);
                syncProgressBar.setVisibility(View.GONE);
                ImageView imgSyncFinished = findViewById(R.id.IMG__SYNC_FINISHED);
                imgSyncFinished.animate()
                        .alpha(1)
                        .scaleX(1)
                        .scaleY(1)
                        .setDuration(500)
                        .withStartAction(() -> {
                            try {
                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                                r.play();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .withEndAction(() -> {
                            txtSyncHint.setText(R.string.txt_hint__sync_finished);
                            buttonStartEvent.setVisibility(View.VISIBLE);
                            txtSyncHint.setVisibility(View.VISIBLE);
                            buttonStartEvent.animate()
                                    .alpha(1)
                                    .setDuration(400)
                                    .start();
                        })
                        .start();
                imgSyncFinished.setVisibility(View.VISIBLE);
            });
            cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        syncTimer.cancel();
    }

    /**
     * Обработчик событий нажатия на кнопки активности
     *
     * @param buttonView объект кнопки
     */
    public void onClick(View buttonView) {
        switch (buttonView.getId()) {
            case buttonSyncId: {
                /* Кнопка "Синхронизировать": отключаем кнопку синхронизации и чекбокс "Автоматическая синхронизация" */
                buttonSync.setEnabled(false);
                findViewById(R.id.CHK__AUTO_SYNC).setEnabled(false);

                /* Показываем троббер синхронизации, подсказку по синхронизации и кнопку начала события */
                syncProgressBar.setVisibility(View.VISIBLE);
                txtSyncHint.setVisibility(View.VISIBLE);
                /* Добавляем в событие точку отсчёта и добавляем конфигурацию */
                long baseTime = System.currentTimeMillis();
                CheckBox chkAutoSync = findViewById(chkAutoSyncId);
                EventConfiguration eventConfiguration = Objects.requireNonNull(EventConfiguration.getCurrent(this));
                if (chkAutoSync.isChecked()) {
                    /* Для автоматической синхронизации округляем в минутах */
                    baseTime = TimeUnit.MILLISECONDS.toMinutes(baseTime) + eventConfiguration.getAutoSyncDelay();
                    baseTime = TimeUnit.MINUTES.toMillis(baseTime);
                } else {
                    /* Для ручной синхронизации округляем в секундах */
                    baseTime = TimeUnit.MILLISECONDS.toSeconds(baseTime) + eventConfiguration.getManualSyncDelay();
                    baseTime = TimeUnit.SECONDS.toMillis(baseTime);
                }
                eventBaseTime = baseTime;
                ApplicationFileManager.getInstance(this).clearTempDir();
                long delay = baseTime - System.currentTimeMillis();
                if (delay < 0) {
                    delay = 0;
                }
                /* Инициализация отложенной задачи завершения синхронизации */
                syncTimer.schedule(new FinishSync(), delay);
                break;
            }

            case buttonStartEventId:
                /* Кнопка "Начать": завершаем текущую активность и начинаем активность "Обработка события" */
                finish();
                startActivity(new Intent(this, ProcessEventActivity.class).putExtra(INTENT_EXTRA_KEY_BASE_TIME, eventBaseTime));
                break;
        }
    }
}