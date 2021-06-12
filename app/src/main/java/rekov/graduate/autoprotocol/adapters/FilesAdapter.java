package rekov.graduate.autoprotocol.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rekov.graduate.autoprotocol.R;
import rekov.graduate.autoprotocol.utils.FileUtils;

/**
 * Расширение адаптера элемента RecyclerView для отображения списка файлов
 */
public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final Context context;
    private final ExecutorService fileOpsExecutor;
    private final ArrayList<File> fileList;
    private final boolean displayChooseButton;

    /**
     * Базовый конструктор файлового адаптера
     * Контекст приложения необходим для подгрузки LayoutInflater, сохранения ссылки на активность и т.д.
     *
     * @param context             контекст приложения
     * @param displayChooseButton флаг отображения кнопки "Выбрать" для файла
     */
    public FilesAdapter(Context context, boolean displayChooseButton) {
        fileList = new ArrayList<>();
        fileOpsExecutor = Executors.newSingleThreadExecutor();
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.displayChooseButton = displayChooseButton;
    }

    /**
     * Асинхронное добавление файла в список
     *
     * @param file файл, который нужно добавить
     */
    public void addFile(File file) {
        fileList.add(file);
        notifyItemInserted(fileList.size() - 1);
    }

    /**
     * Создание ViewHolder из заданного Layout для элемента списка с файлами
     *
     * @param parent   родитель списка (RecyclerView)
     * @param viewType тип view (не применяется)
     * @return ViewHolder созданного элемента списка
     */
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.file_list_item, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Конфигурация элемента списка после привязки ViewHolder
     *
     * @param holder   привязанный ViewHolder
     * @param position позиция в адаптере для элемента списка, к которому привязан ViewHolder
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        /* К полю, отображающему имя файла, привязываем имя файла без расширения */
        String fileName = fileList.get(position).getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        holder.fileNameView.setText(fileName);

        /* Для кнопки "Удалить" устанавливаем обработчик события нажатия, который в фоне удалит файл из системы */
        holder.buttonRemove.setOnClickListener((view) -> {
            int adapterPosition = holder.getAdapterPosition();
            /* Для фонового удаления файла требуется создать копию ссылки на файл, так как оригинальная ссылка удаляется из адаптера */
            File protocolToRemove = new File(fileList.get(adapterPosition).getAbsolutePath());
            fileOpsExecutor.execute(() -> FileUtils.deleteFile(protocolToRemove));
            fileList.remove(adapterPosition);
            notifyItemRemoved(adapterPosition);
        });

        /* Для кнопки "Отправить" устанавливаем обработчик события нажатия, который произведёт запрос отправки файла в ОС */
        holder.buttonSend.setOnClickListener(view -> FileUtils.sendFile(context, fileList.get(holder.getAdapterPosition())));

        /*
         * Если для адаптера установлена опция "Отображать кнопку 'Выбрать'":
         * 1) Отображается соответствующая кнопка
         * 2) На кнопку устанавливается обработчик события нажатия, который:
         *  2.1) Установит результат вызова активности, в которой был порожден адаптер
         *  2.2) В Intent активности положит выбранный файл
         *  2.3) Завершит активность
         */
        if (displayChooseButton) {
            holder.buttonChoose.setVisibility(View.VISIBLE);
            holder.buttonChoose.setOnClickListener(view -> {
                Activity activity = (Activity) context;
                Intent resultFileIntent = new Intent();
                resultFileIntent.setData(Uri.fromFile(fileList.get(holder.getAdapterPosition())));
                activity.setResult(Activity.RESULT_OK, resultFileIntent);
                activity.finish();
            });
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    /**
     * Расширение RecyclerView.ViewHolder для файлового адаптера
     * Содержит следующие элементы:
     * 1) Текстовое поле для отображения имени файла
     * 2) Кнопку "Удалить" (файл)
     * 3) Кнопку "Отправить" (файл)
     * 4) Кнопку "Выбрать" (файл) - отображение данной кнопки опционально и задаётся в конструкторе адаптера
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView fileNameView;
        final Button buttonSend, buttonRemove, buttonChoose;

        ViewHolder(View view) {
            super(view);
            fileNameView = view.findViewById(R.id.file_name);
            buttonRemove = view.findViewById(R.id.btn_remove);
            buttonSend = view.findViewById(R.id.btn_send);
            buttonChoose = view.findViewById(R.id.btn_choose);
        }
    }
}


