package rekov.graduate.autoprotocol.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;

import rekov.graduate.autoprotocol.R;

/**
 * Активность "Настройки"
 * Задача: изменение настроек приложения (язык, тема)
 */
public class SettingsActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        /* Замена Layout-якоря для настроек фрагментом с настройками */
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.LAYOUT__SETTINGS, new SettingsFragment())
                    .commit();
        }

        setActionBarTitle(R.string.title_activity_settings);

        findViewById(R.id.BTN__APPLY_SETTINGS).setOnClickListener(this);
    }

    /**
     * Фрагмент с настройками содержит преференсы из xml-ресурса
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    /**
     * Обработчик нажатия кнопки "Применить настройки"
     *
     * @param buttonView объект нажатой кнопки
     */
    @Override
    public void onClick(View buttonView) {
        if (buttonView.getId() == R.id.BTN__APPLY_SETTINGS) {
            /* При применении настроек текущая активность перезапускается для загрузки ресурсов и перерисовки темы */
            finish();
            startActivity(getIntent());
        }
    }
}