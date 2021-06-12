package rekov.graduate.autoprotocol.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.configurations.PointConfiguration;

/**
 * Активность "Конфигурация точки"
 * Задача: создание и измнение конфигурации контрольной точки
 */
public class PointConfigurationActivity extends BaseActivity implements View.OnClickListener {
    private PointConfiguration pointConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_configuration);
        setActionBarTitle(R.string.title_activity_point_configuration);

        /* Получаем текущую конфигурацию контрольной точки */
        pointConfiguration = PointConfiguration.get(this);
        if (pointConfiguration == null) {
            /* Если конфигурация не задана, то инициализируем новую */
            pointConfiguration = new PointConfiguration(this);
        } else {
            /* Иначе заполняем значениями объекта Конфигурация точки редактируемые поля на форме */
            TextInputLayout field = findViewById(R.id.POINT_CONFIGURATION_FIELD_POINT_IDENTIFIER);
            Objects.requireNonNull(field.getEditText()).setText(String.valueOf(pointConfiguration.getPointId()));
        }

        findViewById(R.id.BTN__APPLY_CONFIGURATION).setOnClickListener(this);
    }

    /**
     * Обработчик события нажатия по кнопке "Применить конфигурацию"
     *
     * @param buttonView объект нажатой кнопки
     */
    @Override
    public void onClick(View buttonView) {
        /* Если фокус на редактируемом поле, то сбрасываем фокус и скрываем клавиатуру */
        if (getCurrentFocus() instanceof EditText) {
            getCurrentFocus().clearFocus();
            InputMethodManager manager = (InputMethodManager) buttonView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(buttonView.getWindowToken(), 0);
        }

        /*
         *  Перенос полей на форме в объект Конфигурация точки и применение конфигурации
         *  В случае неудачи выводится уведомление
         */
        try {
            TextInputLayout field = findViewById(R.id.POINT_CONFIGURATION_FIELD_POINT_IDENTIFIER);
            String pointId = Objects.requireNonNull(field.getEditText()).getText().toString();
            pointConfiguration.setPointId(Integer.parseInt(pointId));
            if (!PointConfiguration.apply(pointConfiguration)) {
                throw new Exception("Could not apply point configuration");
            }
        } catch (Exception ex) {
            Toast.makeText(this, R.string.toast__apply_conf_fail, Toast.LENGTH_LONG).show();
        }
    }
}