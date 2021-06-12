package rekov.graduate.autoprotocol.activities.viewfiles;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.activities.BaseActivity;
import rekov.graduate.autoprotocol.adapters.FilesAdapter;
import rekov.graduate.autoprotocol.utils.ApplicationFileManager;

/**
 * Абстрактная активность просмотра файлов
 * Задача: вывести список с файлами из определенной директории
 *
 * @see #getActionBarTitleResId()
 * @see #getFilesAdapter()
 * @see #getDir()
 */
public abstract class ViewFilesActivity extends BaseActivity {
    /**
     * @return идентификатор ресурса строки с заголовком ActionBar
     */
    protected abstract int getActionBarTitleResId();

    /**
     * @return адаптер списка с файлами для отображения
     * @see FilesAdapter
     */
    protected abstract FilesAdapter getFilesAdapter();

    /**
     * @return директория, файлы которой нужно отображать
     */
    protected abstract String getDir();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);

        /* Заголовок ActionBar устанавливается на основе ресурса, определенного в абстрактном методе */
        setActionBarTitle(getActionBarTitleResId());

        /* Основной RecyclerView, отображающий список файлов, принимает файловый адаптер, определенный в абстрактном методе*/
        RecyclerView listFiles = findViewById(R.id.LIST__FILES);
        FilesAdapter filesAdapter = getFilesAdapter();
        listFiles.setAdapter(filesAdapter);

        /* Поиск файлов в заданной директории приложения */
        File[] files = ApplicationFileManager.getInstance(this).listDirFiles(getDir());
        if (files.length == 0) {
            /* Если файлы отсутствуют, то отображается плейсхолдер */
            findViewById(R.id.TXT__LIST_FILES_PLACEHOLDER).setVisibility(View.VISIBLE);
            listFiles.setVisibility(View.GONE);
        } else {
            /* Иначе в файловый адаптер добавляются все файлы */
            for (File file : files) {
                filesAdapter.addFile(file);
            }
            /*
             *  Регистрация обозревателя изменений адаптера
             *  Если был удалён файл, то проверяем общее количество элементов
             *  Если список файлов пуст, то отображаем плейсхолдер и отвязываем обозреватель
             */
            filesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    if (filesAdapter.getItemCount() == 0) {
                        findViewById(R.id.TXT__LIST_FILES_PLACEHOLDER).setVisibility(View.VISIBLE);
                        listFiles.setVisibility(View.GONE);
                        filesAdapter.unregisterAdapterDataObserver(this);
                    }
                }
            });
        }
    }
}
