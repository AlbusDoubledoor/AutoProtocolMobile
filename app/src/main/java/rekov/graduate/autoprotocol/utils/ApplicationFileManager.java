package rekov.graduate.autoprotocol.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Файловый менеджер приложения - обёртка для работы с файлами приложения
 * Реализует шаблон Одиночка (Singleton)
 */
public class ApplicationFileManager {
    private final File filesDir;
    private final ContentResolver contentResolver;
    private static ApplicationFileManager applicationFileManager;
    private static final String logSource = ApplicationFileManager.class.getSimpleName();
    private static final String tmpDir = "tmp";
    private static final String extDir = "external";
    private static final String objDir = "objects";

    /**
     * Доступ к файловому менеджеру
     *
     * @param context контекст приложения
     * @return инстанс файлового менеджера
     */
    public static ApplicationFileManager getInstance(Context context) {
        return applicationFileManager == null ? (applicationFileManager = new ApplicationFileManager(context)) : applicationFileManager;
    }

    /**
     * Конструктор файлового менеджера
     * Необходимые параметры забираются из контекста
     *
     * @param context контекст приложения
     */
    private ApplicationFileManager(Context context) {
        contentResolver = context.getContentResolver();
        filesDir = context.getFilesDir();
    }


    /**
     * Запись "объектного" файла
     * Объектные файлы содержат объекты и хранятся в отдельной директории
     *
     * @param fileName имя файла
     * @param object   объект, реализующий интерфейс Serializable
     * @return флаг успешного выполнения операции записи
     * @see Serializable
     */
    public boolean writeObjectFile(String fileName, Serializable object) {
        try {
            /* Преобразование объекта в массив байтов с использованием потоков */
            ByteArrayOutputStream objectBytes = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(objectBytes);
            objectOut.writeObject(object);
            objectOut.flush();
            objectOut.close();
            /* Запись массива байтов в директорию с объектными файлами */
            return writeFile(objDir, fileName, objectBytes.toByteArray());
        } catch (IOException ioex) {
            Logger.error(logSource, "Couldn't transform object to bytes for file " + fileName + '\n' + Arrays.toString(ioex.getStackTrace()));
            return false;
        }
    }

    /**
     * Чтение "объектного" файла
     * Объектные файлы содержат объекты и хранятся в отдельной директории
     * Если файл отсутствует или файл не удалось считать, то возвращается null
     *
     * @param fileName имя объектного файла
     * @return считанный объект, реализующий интерфейс Serializable
     * @see Serializable
     */
    @Nullable
    public Serializable readObjectFile(String fileName) {
        /* Считывание файла в массив байтов из директории с объектными файлами */
        byte[] data = readFile(objDir, fileName);
        if (data.length == 0) {
            return null;
        }
        try {
            /* Преобразование массива байтов в объект с использованием потоков */
            ByteArrayInputStream objectBytes = new ByteArrayInputStream(data);
            ObjectInput objInput = new ObjectInputStream(objectBytes);
            return (Serializable) objInput.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            Logger.error(logSource, "Couldn't transform bytes to object for file " + fileName + '\n' + Arrays.toString(ex.getStackTrace()));
            return null;
        }
    }

    /**
     * Удаление всех "объектных" файлов
     * Очищается директория с объектными файлами
     */
    public void deleteObjects() {
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/" + objDir);
        if (dir.exists()) {
            FileUtils.deleteFile(dir);
        }
        Logger.debug(logSource, "Successfully cleared the directory " + dir.getAbsolutePath());
    }

    /**
     * Считывание файла в массив байтов
     *
     * @param dirName  директория, в которой находится файл
     * @param fileName имя файла
     * @return массив считанных байтов
     */
    public byte[] readFile(String dirName, String fileName) {
        File file = new File(filesDir + "/" + dirName + "/" + fileName);
        if (!file.exists()) {
            return new byte[]{};
        }
        try {
            /* Чтение файла как массив байтов с использованием файлового потока */
            FileInputStream inputStream = new FileInputStream(file);
            byte[] data = new byte[inputStream.available()];
            if (inputStream.read(data) < 0) {
                throw new IOException("Null bytes");
            }
            inputStream.close();
            Logger.debug(logSource, "Successfully read the file [" + data.length + " bytes] " + file.getAbsolutePath());
            return data;
        } catch (IOException ioex) {
            Logger.error(logSource, "Couldn't read the file " + file.getAbsolutePath() + "\n" + Arrays.toString(ioex.getStackTrace()));
            return new byte[]{};
        }
    }

    /**
     * Метод получения "внешнего" файла из Uri
     * Внешние файлы предоставляются поставщиком файлов (FileProvider) и не могут быть непосредственно считаны методами файловой обработки
     * Для дальнейшей обработки таких файлов содержимое копируется байтами (через ContentResolver) во временный файл внутренний директории приложения
     *
     * @param uri URI файла, по которому можно считать содержимое (предоставляется файловым поставщиком (FileProvider))
     * @return ссылка на временный файл (содержимое скопировано) внутри директории приложения
     */
    public File getExternalFile(Uri uri) {
        File result = null;
        try {
            /* Чтение содержимого файла в массив байтов с использованием потока ContentResolver */
            InputStream inputStream = contentResolver.openInputStream(uri);
            byte[] data = new byte[inputStream.available()];
            if (inputStream.read(data) >= 0) {
                String fileName = new File(uri.getPath()).getName();
                /* Запись содержимого во временный файл внутри директории приложения и возвращение ссылки на созданный файл */
                if (applicationFileManager.writeTempFile(extDir, fileName, data)) {
                    result = new File(filesDir + "/" + tmpDir + "/" + extDir + "/" + fileName);
                }
            } else {
                throw new Exception("No available data");
            }
        } catch (Exception ex) {
            Logger.error(logSource, "Couldn't get external file " + uri.getPath() + '\n' + Arrays.toString(ex.getStackTrace()));
        }
        return result;
    }

    /**
     * Очистка временной директории с "внешними файлами"
     *
     * @see #getExternalFile(Uri)
     */
    public void clearExternalDir() {
        clearTempDir(extDir);
    }

    /**
     * Вывод списка файлов внутри директории приложении
     *
     * @param dirName директория приложения, файлы из которой нужно получить
     * @return массив файлов
     */
    public File[] listDirFiles(String dirName) {
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/" + dirName);
        return dir.exists() ? dir.listFiles() : new File[]{};
    }

    /**
     * Считывание части файла внутри приложения в список строк
     * Часть файла, которую нужно считать, должно быть задано старт-строкой и стоп-строкой
     *
     * @param file      файл приложения, который нужно считать
     * @param startLine старт-строка, с которой начинается чтение файла (не вкл. в результат)
     * @param stopLine  стоп-строка, на которой заканчивается чтение файла (не вкл. в результат)
     * @return список считанных строк из файла
     */
    public ArrayList<String> readFile(File file, String startLine, String stopLine) {
        ArrayList<String> result = new ArrayList<>();
        try {
            /* Построчное считывание файла с использованием буферизованного ридера, связанного с файловым ридером */
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (br.ready()) {
                if (br.readLine().equals(startLine)) {
                    break;
                }
            }
            while (br.ready()) {
                String line = br.readLine();
                if (line.equals(stopLine)) {
                    break;
                }
                result.add(line);
            }
            br.close();
            Logger.debug(logSource, "Successfully read the file [" + result.size() + " lines] " + file.getAbsolutePath());
        } catch (IOException ioex) {
            Logger.error(logSource, "Couldn't read the file " + file.getAbsolutePath() + "\n" + Arrays.toString(ioex.getStackTrace()));
        }
        return result;
    }

    /**
     * Считывание всех файлов из временной директории внутри приложения в список строк
     *
     * @param tmpDirName имя временной директории, из который нужно считать файлы
     * @return список строк из всех файлов
     */
    public ArrayList<String> readTempDir(String tmpDirName) {
        ArrayList<String> result = new ArrayList<>();
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/tmp/" + tmpDirName);
        if (!dir.exists()) {
            Logger.debug(logSource, "Successfully read the directory [" + result.size() + " lines] " + dir.getAbsolutePath());
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            Logger.debug(logSource, "Successfully read the directory [" + result.size() + " lines] " + dir.getAbsolutePath());
            return result;
        }

        /* Построчное считывание всех файлов в директории с использованием буферизованного ридера, связанного с файловым ридером */
        BufferedReader br;
        try {
            for (File file : files) {
                br = new BufferedReader(new FileReader(file));
                while (br.ready()) {
                    result.add(br.readLine());
                }
                br.close();
            }
            Logger.debug(logSource, "Successfully read the directory [" + result.size() + " lines] " + dir.getAbsolutePath());
        } catch (IOException ioex) {
            Logger.error(logSource, "Couldn't read the directory " + dir.getAbsolutePath() + "\n" + Arrays.toString(ioex.getStackTrace()));
        }
        return result;
    }

    /**
     * Очистка временной директории
     *
     * @param tmpDirName имя директории
     */
    public void clearTempDir(String tmpDirName) {
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/" + tmpDir + "/" + tmpDirName);
        if (dir.exists()) {
            FileUtils.deleteFile(dir);
        }
        Logger.debug(logSource, "Successfully cleared the directory " + dir.getAbsolutePath());
    }

    /**
     * Очистка всех временных директорий со всеми временными файлами
     */
    public void clearTempDir() {
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/" + tmpDir);
        if (dir.exists()) {
            FileUtils.deleteFile(dir);
        }
        Logger.debug(logSource, "Successfully cleared the directory " + dir.getAbsolutePath());
    }


    /**
     * Запись строки данных во "временный файл" внутри приложения
     *
     * @param tmpDirName специальная временная директория для файла
     * @param fileName   имя файла
     * @param data       строка с данными, которую нужно записать
     * @return флаг успешной операции записи
     */
    public boolean writeTempFile(String tmpDirName, String fileName, String data) {
        return writeFile(tmpDir + "/" + tmpDirName, fileName, data);
    }

    /**
     * Запись массива байтов во "временный файл" внутри приложения
     *
     * @param tmpDirName специальная временная директория для файла
     * @param fileName   имя файла
     * @param data       массив байтов, который нужно записать
     * @return флаг успешной операции записи
     */
    public boolean writeTempFile(String tmpDirName, String fileName, byte[] data) {
        return writeFile(tmpDir + "/" + tmpDirName, fileName, data);
    }

    /**
     * Запись строки данных в файл внутри приложения
     *
     * @param dirName  директория внутри приложения
     * @param fileName имя файла
     * @param data     строка с данными, которую нужно записать
     * @return флаг успешной операции записи
     */
    public boolean writeFile(String dirName, String fileName, String data) {
        return writeFile(dirName, fileName, data.getBytes());
    }

    /**
     * Запись массива байтов в файл внутри приложения
     *
     * @param dirName  директория внутри приложения
     * @param fileName имя файла
     * @param data     массив байтов, который нужно записать
     * @return флаг успешной операции записи
     */
    public boolean writeFile(String dirName, String fileName, byte[] data) {
        File dir = filesDir;
        dir = new File(dir.getAbsolutePath() + "/" + dirName);
        /* Создание директорий */
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Logger.error(logSource, "Couldn't create a directory " + dir.getAbsolutePath());
                return false;
            }
        }
        File file = new File(dir, fileName);
        try {
            /* Создание файла */
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Logger.error(logSource, "Couldn't create a file " + file.getAbsolutePath());
                    return false;
                }
            }

            /* Запись массива байтов в файл с использованием файлового потока вывода */
            OutputStream outStream = new FileOutputStream(file);
            outStream.write(data);
            outStream.close();
            Logger.debug(logSource, "Successfully wrote a file " + file.getAbsolutePath());
        } catch (IOException ioex) {
            Logger.error(logSource, "Couldn't create a file " + file.getAbsolutePath());
            return false;
        }
        return true;
    }
}
