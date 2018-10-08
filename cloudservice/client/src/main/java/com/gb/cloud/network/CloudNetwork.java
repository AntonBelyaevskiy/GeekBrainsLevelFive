package com.gb.cloud.network;

import com.gb.cloud.message.CloudCommand;
import com.gb.cloud.message.CloudFile;
import com.gb.cloud.message.CloudFilesList;
import com.gb.cloud.tableViewElements.ElementForTableView;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

//Класс в котором содержится бизнес-логика приложения
public class CloudNetwork {

    private volatile static CloudNetwork network;

    private Socket socket;
    private ObjectEncoderOutputStream encoder;
    private ObjectDecoderInputStream decoder;

    private ArrayList<ElementForTableView> cloudStorage;

    private CloudNetwork() {
    }

    public static CloudNetwork getNetwork() {
        if (network == null) {
            synchronized (CloudNetwork.class) {
                if (network == null) {
                    network = new CloudNetwork();
                }
            }
        }
        return network;
    }

    public void run(final int port, final String ip, String login) {
        try {
            socket = new Socket(ip, port);
            encoder = new ObjectEncoderOutputStream(socket.getOutputStream());
            decoder = new ObjectDecoderInputStream(socket.getInputStream());

            encoder.writeObject(new CloudFilesList(login));

            //клиент в бесконечном цикле, будет слушать всё, что присылает ему сервер
            //запускаем в отдельном потоке, чтобы не блокировать выполнение работы клиента
            Thread authThread = new Thread(() -> {
                try {
                    while (true) {
                        //логика клиента
                        Object message = decoder.readObject();

                        //если с сервера прилетает список файлов, обновляем список на клиенте
                        if (message instanceof CloudFilesList)
                            takeFilesListFromServer((CloudFilesList) message);

                        if (message instanceof CloudCommand) {
                            if (((CloudCommand) message).getCommandName().equals("/exit"))
                                System.exit(0);
                            else
                                doCommand((CloudCommand) message);
                        }
                        if (message instanceof CloudFile)
                            saveFile((CloudFile) message);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                        encoder.close();
                        decoder.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("disconnect");
                }
            });

            //когда логинимся на клиенте, то сперва показываем актуальный список файлов в облаке
            //после чего запускаем поток на прослушивание сервера
            Object message = decoder.readObject();
            if (message instanceof CloudFilesList) {
                takeFilesListFromServer((CloudFilesList) message);
            }
            authThread.setDaemon(true);
            authThread.start();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //получить список файлов в хранилище на сервере
    private void takeFilesListFromServer(CloudFilesList message) {
        System.out.println("пришел список файлов");
        cloudStorage = message.getServerStorageList();
    }

    //обрабатываем команды, которые прилетают с сервера
    private void doCommand(CloudCommand message) {
        System.out.println(message.getCommandName());

        if (message.getCommandName().equals("/updateList")) {
            //сделать автоматическое обновление TableView при изменении его содержимого
            //оповистить клиента о загрузке файла
            System.out.println("список обновился");
        }
    }

    //если из облака прилетает файл, то сохраняем его на локальном репозитории
    private void saveFile(CloudFile message) {
        Path path = Paths.get("client/storage/" + message.getName());
        byte[] content = message.getContent();

        if (Files.exists(path)) {
            try {
                Files.delete(path);
                Files.write(path, content, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Files.write(path, content, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<ElementForTableView> getCloudFilesStorage() {
        return cloudStorage;
    }

    //метод, который отправляет наш файл в облако, методу необходимо сообщить название отправляемого файла
    public void uploadFileOnCloud(String name) {
        CloudFile file = makeFile(name);
        try {
            encoder.writeObject(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadFileFromCloud(String name) {
        CloudCommand command = new CloudCommand("/downloadFile", name);
        try {
            encoder.writeObject(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //метод при помощи, которого создаём объект - "файл для отправки" по названию файла
    private CloudFile makeFile(String name) {
        byte[] content = new byte[0];
        try {
            //считываем содержимое файла из хранилища, по его названию
            content = Files.readAllBytes(Paths.get("client/storage/" + name));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CloudFile(name, content);
    }

    public void deleteFileOnCloudStorage(String name) {
        CloudCommand command = new CloudCommand("/deleteFile", name);
        try {
            encoder.writeObject(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
