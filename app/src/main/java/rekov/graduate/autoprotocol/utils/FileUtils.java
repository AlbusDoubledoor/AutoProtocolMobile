package rekov.graduate.autoprotocol.utils;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

import rekov.graduate.autoprotocol.BuildConfig;
import rekov.graduate.autoprotocol.R;

/**
 * Класс, предоставляющий утилиты для работы с файлами и файловым поставщиком (FileProvider)
 */
public class FileUtils {
    private static final String logSource = FileUtils.class.getSimpleName();
    private static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";
    private static final String SEND_FILE_MIME_TYPE = "text/plain";
    private static final String FILENAME_RESERVED_CHARS_PATTERN = "[\\s|\\\\?*<\":>+\\[\\]/']";

    /**
     * Удаление директории/файла
     *
     * @param file директория или файл, который требуется удалить
     */
    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteFile(entry);
                }
            }
        }
        if (!file.delete()) {
            Logger.error(logSource, "Failed to delete " + file.getAbsolutePath());
        }
    }

    /**
     * Получение URI файла с использованием файлового поставщика (FileProvider)
     *
     * @param context контекст приложения
     * @param file    файл, для которого требуется сформировать URI
     * @return URI файла
     */
    public static Uri getUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file);
    }

    /**
     * Запрос отправки файла (в другое приложение)
     *
     * @param context контекст приложения (требуется для использования файлового поставщика)
     * @param file    файл, который требуется отправить
     */
    public static void sendFile(Context context, File file) {
        Intent intentSendFile = new Intent(Intent.ACTION_SEND);

        if (file.exists()) {
            intentSendFile.setType(SEND_FILE_MIME_TYPE);

            Uri contentUri = getUriForFile(context, file);

            String extraSubject = context.getString(R.string.intent_subject__send_file);
            intentSendFile.setClipData(new ClipData(
                    extraSubject,
                    new String[]{SEND_FILE_MIME_TYPE},
                    new ClipData.Item(contentUri)
            ));
            intentSendFile.putExtra(Intent.EXTRA_SUBJECT, extraSubject);
            intentSendFile.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.intent_text__send_file));
            intentSendFile.putExtra(Intent.EXTRA_STREAM, contentUri);

            /*
             *  Флаги предоставления разршения на чтение/запись файлов
             *  Флаги требуется проставить для обоих Intent (оригинального и chooser)
             *  Это позволяет отправить файл через любое приложение и через Bluetooth, Wi-Fi Direct, etc..
             */
            intentSendFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intentSendFile.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intentSendFile, extraSubject);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooser.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            context.startActivity(chooser);
        }
    }

    /**
     * Экранирование имени файла для безопасного использования в операционной системе
     *
     * @param unsafeFileName исходное неэкраинрованное имя файла
     * @return имя, в котором удалены все зарезервированные символы
     */
    public static String escape(String unsafeFileName) {
        return unsafeFileName.replaceAll(FILENAME_RESERVED_CHARS_PATTERN, "");
    }
}
