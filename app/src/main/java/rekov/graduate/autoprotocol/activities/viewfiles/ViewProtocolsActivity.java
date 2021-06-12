package rekov.graduate.autoprotocol.activities.viewfiles;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.adapters.FilesAdapter;
import rekov.graduate.autoprotocol.protocol.Protocol;

/**
 * Активность "Просмотр файлов протоколов"
 * Задача: вывести список с сохраненными протоколами
 *
 * @see ViewFilesActivity реализация логики Activity
 */
public class ViewProtocolsActivity extends ViewFilesActivity {
    @Override
    protected int getActionBarTitleResId() {
        return R.string.title_activity_view_protocols;
    }

    @Override
    protected FilesAdapter getFilesAdapter() {
        return new FilesAdapter(this, false);
    }

    @Override
    protected String getDir() {
        return Protocol.DIR;
    }
}