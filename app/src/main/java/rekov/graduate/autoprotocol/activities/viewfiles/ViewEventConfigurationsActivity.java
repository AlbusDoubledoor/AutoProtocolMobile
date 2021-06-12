package rekov.graduate.autoprotocol.activities.viewfiles;


import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.adapters.FilesAdapter;
import rekov.graduate.autoprotocol.configurations.EventConfiguration;

/**
 * Активность "Просмотр файлов конфигурации события"
 * Задача: вывести список с сохраненными файлами конфигурации события
 *
 * @see ViewFilesActivity реализация логики Activity
 */
public class ViewEventConfigurationsActivity extends ViewFilesActivity {
    @Override
    protected int getActionBarTitleResId() {
        return R.string.title_activity_view_event_configurations;
    }

    @Override
    protected FilesAdapter getFilesAdapter() {
        return new FilesAdapter(this, true);
    }

    @Override
    protected String getDir() {
        return EventConfiguration.DIR;
    }
}
