package rekov.graduate.autoprotocol.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.activities.viewfiles.ViewEventConfigurationsActivity;
import rekov.graduate.autoprotocol.configurations.EventConfiguration;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;
import rekov.graduate.autoprotocol.utils.DateTimeFormatter;
import rekov.graduate.autoprotocol.utils.FileUtils;

/**
 * Активность "Конфигурация события"
 * Задача: создание и изменение конфигураций события
 */
public class EventConfigurationActivity extends BaseActivity implements View.OnClickListener {
    private static final int MENU_ITEM_ID__LOAD_FROM_DEVICE = R.id.MENU_ITEM__LOAD_FROM_DEVICE;
    private static final int MENU_ITEM_ID__LOAD_FROM_PREVIOUS = R.id.MENU_ITEM__LOAD_FROM_PREVIOUS;
    private static final int MENU_ITEM_ID__CREATE_MANUALLY = R.id.MENU_ITEM__CREATE_MANUALLY;
    private static final int BUTTON_ID__LOAD_EVENT_CONFIGURATION = R.id.BTN__LOAD_EVENT_CONFIGURATION;
    private static final int BUTTON_ID__APPLY_CONFIGURATION = R.id.BTN__APPLY_CONFIGURATION;
    private static final int BUTTON_ID__SAVE_CONFIGURATION = R.id.BTN__SAVE_CONFIGURATION;
    private static final String DEFAULT_FILE_PLACEHOLDER = "event_configuration";
    private EventConfiguration eventConfiguration;
    private ApplicationFileManager applicationFileManager;
    private PopupMenu loadConfigurationPopup;
    private RelativeLayout eventConfLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_configuration);
        /* Скрываем ActionBar */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        eventConfLayout = findViewById(R.id.LAYOUT__EVENT_CONFIGURATION_FIELDS);
        applicationFileManager = ApplicationFileManager.getInstance(this);

        /* Получаем текущую конфигурацию события: если конфигурация задана, то отображаем */
        eventConfiguration = EventConfiguration.getCurrent(this);
        if (eventConfiguration != null) {
            displayConfiguration();
        }

        findViewById(BUTTON_ID__APPLY_CONFIGURATION).setOnClickListener(this);
        findViewById(BUTTON_ID__SAVE_CONFIGURATION).setOnClickListener(this);
        findViewById(BUTTON_ID__LOAD_EVENT_CONFIGURATION).setOnClickListener(this);

        /* Для кнопки "Загрузить конфигурацию" добавлем всплывающее меню */
        loadConfigurationPopup = new PopupMenu(this, findViewById(R.id.BTN__LOAD_EVENT_CONFIGURATION));
        MenuInflater inflater = loadConfigurationPopup.getMenuInflater();
        inflater.inflate(R.menu.event_configuration_menu, loadConfigurationPopup.getMenu());
        loadConfigurationPopup.setGravity(Gravity.END);

        /*
         *  Реализация логики по реакции на событие клика по элементу всплывающего меню
         *  1) Если элемент - "Загрузить с устройства", то запрашиваем файл у операционной системы
         *  2) Если элемент - "Загрузить из предыдущих", то запускаем активность просмотра файлов конфигурации
         *  3) Если элемент - "Создать вручную", то инициализируем новую конфигурацию и отображаем её
         */
        loadConfigurationPopup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case MENU_ITEM_ID__LOAD_FROM_DEVICE:
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.intent_text__select_file)), REQUEST_CODE_FILE_EXTERNAL);
                    break;
                case MENU_ITEM_ID__LOAD_FROM_PREVIOUS:
                    startActivityForResult(new Intent(this, ViewEventConfigurationsActivity.class), REQUEST_CODE_FILE_INTERNAL);
                    break;
                case MENU_ITEM_ID__CREATE_MANUALLY:
                    EventConfiguration.Builder builder = new EventConfiguration.Builder(this);
                    eventConfiguration = builder.create();
                    displayConfiguration();
                    break;
            }
            return true;
        });
    }

    /**
     * Отображение конфигурации
     * Значения из объекта "Конфигурация события" переносятся в редактируемые поля на форме
     */
    private void displayConfiguration() {
        eventConfLayout.setVisibility(View.VISIBLE);
        HashMap<Integer, String> fields = eventConfiguration.getFields();
        for (HashMap.Entry<Integer, String> fieldEntry : fields.entrySet()) {
            EditText editText = ((TextInputLayout) findViewById(fieldEntry.getKey())).getEditText();
            if (editText != null) {
                editText.setText(fieldEntry.getValue());
            }
        }
    }

    /**
     * Обработчик событий клика по кнопкам активности
     *
     * @param buttonView кнопка, по которой совершено нажатие
     */
    @Override
    public void onClick(View buttonView) {
        switch (buttonView.getId()) {
            case BUTTON_ID__LOAD_EVENT_CONFIGURATION:
                /* Кнопка "Загрузить конфигурацию" - вызов всплывающего меню с вариантами загрузки */
                loadConfigurationPopup.show();
                break;
            case BUTTON_ID__SAVE_CONFIGURATION:
            case BUTTON_ID__APPLY_CONFIGURATION:
                /*
                 *  Кнопки "Сохранить конфигурацию" и "Применить конфигурацию"
                 *  Значения редактируемых полей с формы переносятся в объект "Конфигурация события" и валидируются
                 */
                String validateMessage = setConfiguration();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                if (validateMessage.equals("")) {
                    if (buttonView.getId() == BUTTON_ID__APPLY_CONFIGURATION) {
                        /* Связывание объекта "Конфигурация события" с текущей конфигурацией события */
                        if (!EventConfiguration.apply(eventConfiguration)) {
                            Toast.makeText(this, "Can't apply event configuration", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        /*
                         * Для кнопки "Сохранить конфигурацию" вызывается диалоговое окно с предложением заполнить имя файла
                         * Если поле остается пустым, то применяется значение по умолчанию (плейсхолдер + дата)
                         */
                        dialogBuilder
                                .setTitle(R.string.dlg_title__choose_configuration_file_name)
                                .setView(R.layout.layout_file_name)
                                .setPositiveButton(R.string.dlg_btn__save, (dialog, which) -> {
                                });
                        AlertDialog dialog = dialogBuilder.create();
                        dialog.show();
                        String fileNamePlaceHolder = DEFAULT_FILE_PLACEHOLDER + "_" + DateTimeFormatter.formatDate(System.currentTimeMillis());
                        EditText fileNameEdit = dialog.findViewById(R.id.file_name);
                        Objects.requireNonNull(fileNameEdit).setHint(fileNamePlaceHolder);
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener((positiveView) -> {
                            String fileName = fileNameEdit.getText().toString();
                            if (fileName.equals("")) {
                                fileName = fileNameEdit.getHint().toString();
                            }
                            fileName = FileUtils.escape(fileName);
                            if (!eventConfiguration.writeFile(fileName)) {
                                /* Если сохранить конфигурацию не удалось, то вызывается диалоговое окно с уведомлением */
                                AlertDialog.Builder operationFailedNotify = new AlertDialog.Builder(this);
                                operationFailedNotify.setMessage(R.string.dlg_msg__conf_write_fail);
                                operationFailedNotify.setPositiveButton(R.string.dlg_btn__close, (subDialog, subWhich) -> {
                                });
                                dialog.dismiss();
                                operationFailedNotify.create().show();
                            } else {
                                dialog.dismiss();
                            }
                        });
                    }
                } else {
                    /* Если валидация конфигурации провалилась, то вызывается диалоговое окно с подробностями валидации */
                    dialogBuilder
                            .setMessage(getString(R.string.dlg_msg__conf_validation_fail) + validateMessage)
                            .setPositiveButton(R.string.dlg_btn__close, (dialog, which) -> {
                            });
                    dialogBuilder.create().show();
                }
                break;
        }
    }

    /**
     * Установка конфигурации: значения редактируемых полей с формы переносятся в объект "Конфигурация события" и валидируются
     *
     * @return сообщение с подробностями валидации (или пустая строка, если валидация успешна)
     */
    private String setConfiguration() {
        if (eventConfiguration == null) {
            EventConfiguration.Builder builder = new EventConfiguration.Builder(this);
            eventConfiguration = builder.create();
        }
        StringBuilder validator = new StringBuilder();
        HashMap<Integer, String> getFields = eventConfiguration.getFields();
        HashMap<Integer, String> setFields = new HashMap<>(getFields);
        for (HashMap.Entry<Integer, String> fieldEntry : getFields.entrySet()) {
            EditText editText = ((TextInputLayout) findViewById(fieldEntry.getKey())).getEditText();
            if (editText != null) {
                String text = editText.getText().toString();
                if (text.equals("")) {
                    validator.append(editText.getHint()).append('\n');
                    continue;
                }
                setFields.put(fieldEntry.getKey(), editText.getText().toString());
            }
        }
        if (validator.length() == 0) {
            if (!eventConfiguration.setFields(setFields)) {
                validator.append("@Exception");
            }
        }
        return validator.toString().trim();
    }

    /**
     * Обработчик результатов вызванных активностей
     *
     * @param requestCode код вызова
     * @param resultCode  код результата
     * @param data        возвращенные данные
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        Uri uri = data.getData();
        switch (requestCode) {
            case REQUEST_CODE_FILE_EXTERNAL:
                /* Для загрузки файла с устройства (внешнее хранилище) используем обертку файлового менеджера приложения */
                File tmpFile = applicationFileManager.getExternalFile(uri);
                uri = Uri.fromFile(tmpFile);
            case REQUEST_CODE_FILE_INTERNAL:
                String path = uri.getPath();
                File file = new File(path);
                String fileName = file.getName();
                /*
                 * Валидация загруженного файла
                 * Если расширение файла отличается от нужного, то происходит сброс и конфигурации и выводится уведомление
                 */
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex < 0 || !fileName.substring(dotIndex).equals(EventConfiguration.FILE_EXTENSION)) {
                    Toast.makeText(this, getString(R.string.toast__support_format, EventConfiguration.FILE_EXTENSION), Toast.LENGTH_LONG).show();
                    eventConfiguration = null;
                    eventConfLayout.setVisibility(View.INVISIBLE);
                    break;
                }

                /*
                 * Если файл успешно загружен, то происходит попытка инициализации нового объекта "Конфигурация события"
                 * В случае успешной инициализации - значения объекта выводятся на форму
                 * Иначе - конфигурация сбрасывается и выводится уведомление
                 */
                try {
                    EventConfiguration.Builder builder = new EventConfiguration.Builder(file, this);
                    eventConfiguration = builder.create();
                    displayConfiguration();
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.toast__read_conf_fail, Toast.LENGTH_LONG).show();
                    eventConfiguration = null;
                    eventConfLayout.setVisibility(View.INVISIBLE);
                }
                break;
        }
        applicationFileManager.clearExternalDir();
    }
}