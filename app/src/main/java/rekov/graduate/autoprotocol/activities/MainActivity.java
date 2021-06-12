package rekov.graduate.autoprotocol.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.activities.viewfiles.ViewProtocolsActivity;
import rekov.graduate.autoprotocol.configurations.EventConfiguration;
import rekov.graduate.autoprotocol.configurations.PointConfiguration;

/**
 * Основная активность приложения (точка входа)
 * Содержит кнопки для запуска других активностей (Настройки, Конфигурации, Старт события)
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static final int BUTTON_ID__START = R.id.BTN__START;
    private static final int BUTTON_ID__EVENT_CONFIGURATION = R.id.BTN__EVENT_CONFIGURATION;
    private static final int BUTTON_ID__POINT_CONFIGURATION = R.id.BTN__POINT_CONFIGURATION;
    private static final int BUTTON_ID__VIEW_PROTOCOLS = R.id.BTN__VIEW_PROTOCOLS;
    private static final int BUTTON_ID__EXIT = R.id.BTN__EXIT;
    private static final int[] allButtonsIds = new int[]{BUTTON_ID__START, BUTTON_ID__EVENT_CONFIGURATION, BUTTON_ID__POINT_CONFIGURATION, BUTTON_ID__VIEW_PROTOCOLS, BUTTON_ID__EXIT};
    private static String language;
    private static String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            /* Для главной активности отключаем кнопку "Назад" и заголовок на ActionBar */
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        for (int buttonId : allButtonsIds) {
            /* Всем кнопкам на активности назначаем общий обработчик нажатия */
            findViewById(buttonId).setOnClickListener(this);
        }
    }

    /**
     * Обработчик нажатия кнопок
     * 1) Кнопка "Старт" начинает активность Синхронизации
     * 2) Кнопка "Конфигурация события" начинает активность Конфигурации события
     * 3) Кнопка "Конфигурация точки" начинает активность Конфигурация контрольной точки
     * 4) Кнопка "Файлы протоколов" начинает активность Просмотр файлов протоколов
     * 5) Кнопка "Выход" завершает текущую активность (выход из программы)
     *
     * @param buttonView объект нажатой кнопки
     */
    public void onClick(View buttonView) {
        switch (buttonView.getId()) {
            case BUTTON_ID__START:
                /* Если отсутствует Конфигурация события, то выводится диалоговое окно с уведомлением */
                if (EventConfiguration.getCurrent(this) == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.dlg_msg__event_configuration_missing)
                            .setPositiveButton(R.string.dlg_btn__close, (dialog, which) -> {
                            });
                    builder.create().show();
                    break;
                }
                /* Если отсутствует Конфигурация точки, то выводится диалоговое окно с уведомлением */
                if (PointConfiguration.get(this) == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.dlg_msg__point_configuration_missing)
                            .setPositiveButton(R.string.dlg_btn__close, (dialog, which) -> {
                            });
                    builder.create().show();
                    break;
                }

                startActivity(new Intent(this, SyncActivity.class));
                break;
            case BUTTON_ID__EVENT_CONFIGURATION:
                startActivity(new Intent(this, EventConfigurationActivity.class));
                break;
            case BUTTON_ID__POINT_CONFIGURATION:
                startActivity(new Intent(this, PointConfigurationActivity.class));
                break;
            case BUTTON_ID__VIEW_PROTOCOLS:
                startActivity(new Intent(this, ViewProtocolsActivity.class));
                break;
            case BUTTON_ID__EXIT:
                finish();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Добавляем меню с кнопкой "Настройки" */
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Запуск активности "Настройки"
     */
    private void startSettingsActivity() {
        language = getCurrentLanguage();
        theme = getCurrentTheme();
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_SETTINGS);
    }

    /**
     * Обработчик результатов активностей (например, Настроек)
     *
     * @param requestCode код запроса, иниицировавший активность
     * @param resultCode  код результата выполнения активности (RESULT_OK, RESULT_CANCELLED, либо свой)
     * @param data        дополнительные данные, возвращаемые активностью
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /* Если во время активности "Настройки" были изменены язык или тема, то текущую активность нужно перерисовать (перезапустить)*/
        if (requestCode == REQUEST_CODE_SETTINGS) {
            if (!(language.equals(getCurrentLanguage()) && theme.equals(getCurrentTheme()))) {
                finish();
                startActivity(getIntent());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Если в меню была нажата кнопка "Настройки" (шестерёнка), то запускаем соотв. активность */
        if (item.getItemId() == R.id.MENU_ITEM__SETTINGS) {
            startSettingsActivity();
            return true;
        }
        return false;
    }
}