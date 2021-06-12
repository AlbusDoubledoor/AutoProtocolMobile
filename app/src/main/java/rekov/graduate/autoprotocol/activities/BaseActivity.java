package rekov.graduate.autoprotocol.activities;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import rekov.graduate.autoprotocol.R;

/**
 * Класс для реализации основных общих методов всех активностей в приложении
 */
public class BaseActivity extends AppCompatActivity {
    private static final String THEME_DAY = "day";
    private static final String THEME_NIGHT = "night";
    private static final String DEFAULT_LOCALE = "ru";
    private static final String DEFAULT_THEME = THEME_DAY;
    protected static final String INTENT_EXTRA_KEY_BASE_TIME = "EVENT";
    protected static final int REQUEST_CODE_SETTINGS = 1;
    protected static final int REQUEST_CODE_FILE_EXTERNAL = 2;
    protected static final int REQUEST_CODE_FILE_INTERNAL = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyPreferences();

        /* По умолчанию показываем на всех активностях кнопку "Назад" в ActionBar */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Для кнопки "Назад" на ActionBar реализуем стандартное завершение активности */
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    /**
     * Применение настроек приложения (язык, тема)
     */
    private void applyPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        setLocale(preferences);
        setTheme(preferences);
    }

    /**
     * Установка темы на основе значения настройки темы (день/ночь)
     * По умолчанию устанавливается дневной режим
     *
     * @param preferences объект класса SharedPreferences, в котором содержится preference "theme"
     */
    private void setTheme(SharedPreferences preferences) {
        String theme = preferences.getString(getString(R.string.prefs__theme), DEFAULT_THEME);
        int mode = theme.equals(THEME_NIGHT) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Установка локали приложения на основе значения настройки языка
     * По умолчанию устанавливается русский язык (ru)
     *
     * @param preferences объект класса SharedPreferences, в котором содержится preference "language"
     */
    private void setLocale(SharedPreferences preferences) {
        String lang = preferences.getString(getString(R.string.prefs__language), DEFAULT_LOCALE);
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    /**
     * Получение текущего установленного языка (ru/en/...)
     *
     * @return языковая локаль в спецификации ISO-639-1
     */
    protected String getCurrentLanguage() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(getString(R.string.prefs__language), DEFAULT_LOCALE);
    }

    /**
     * Получение текущей установленной темы UI
     *
     * @return day/night
     */
    protected String getCurrentTheme() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getString(getString(R.string.prefs__theme), DEFAULT_THEME);
    }

    /**
     * Установка заголовка ActionBar с проверкой на null
     *
     * @param titleResourceId идентификатор ресурса, содержащего заголовок
     */
    protected void setActionBarTitle(int titleResourceId) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(titleResourceId);
        }
    }
}
