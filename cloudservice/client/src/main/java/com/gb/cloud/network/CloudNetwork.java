package com.gb.cloud.network;

import com.gb.cloud.controller.CloudController;
import com.gb.cloud.message.*;
import com.gb.cloud.tableViewElements.ElementForTableView;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

//Класс в котором содержится бизнес-логика приложения
public class CloudNetwork {

    private static final int PORT = 8189;
    private static final String SERVER_IP = "localhost";

    private static volatile CloudNetwork network;
    private static CloudController controller;

    private static Socket socket;

    private ObjectEncoderOutputStream encoder;
    private ObjectDecoderInputStream decoder;

    private CloudNetwork() {

    }

    public static CloudNetwork getNetwork(CloudController connectController) {
        if (network == null) {
            synchronized (CloudNetwork.class) {
                if (network == null) {
                    network = new CloudNetwork();
                    controller = connectController;
                }
            }
        }
        return network;
    }

    public void run() {

        try {
            socket = new Socket(SERVER_IP, PORT);
            encoder = new ObjectEncoderOutputStream(socket.getOutputStream());
            decoder = new ObjectDecoderInputStream(socket.getInputStream());

            //клиент в бесконечном цикле, будет слушать всё, что присылает ему сервер
            //запускаем в отдельном потоке, чтобы не блокировать выполнение работы клиента
            Thread workThread = new Thread(() -> {
                try {
                    while (true) {
                        //логика клиента
                        Object message = decoder.readObject();


                        //если с сервера прилетает список файлов, обновляем список на клиенте
                        if (message instanceof CloudFilesList) {
                            CloudFilesList msg = (CloudFilesList) message;
                            CloudController.setCloudStorage(msg.getServerStorageList());
                            controller.drawCloudTableView();

                        }

                        if (message instanceof CloudCommand) {
                            if (((CloudCommand) message).getCommandName().equals("/exit"))
                                System.exit(0);
                            else
                                doCommand((CloudCommand) message);
                        }
                        if (message instanceof CloudSmallFile) {
                            saveSmallFile((CloudSmallFile) message);
                        }
                        if (message instanceof CloudLargeFile) {
                            saveLargeFile((CloudLargeFile) message);
                        }

                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            });
            workThread.setDaemon(true);
            workThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            socket.close();
            encoder.close();
            decoder.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("disconnect");
    }

    //обрабатываем команды, которые прилетают с сервера
    private void doCommand(CloudCommand message) {
        System.out.println(message.getCommandName());

        if (message.getCommandName().equals("/allowToEnter")) {
            controller.showCloudWindowAfterLogin();
            controller.drawLocalTableView();
            controller.drawCloudTableView();
        }

        if (message.getCommandName().equals("/busyLogin")) {
            controller.serviceMessage.setText("Пользователь с таким логином уже существует");
            controller.loginRegisterTextField.setText("");
            controller.passwordRegisterTextField.setText("");
        }

        if (message.getCommandName().equals("/wrongLoginOrPassword")) {
            controller.serviceMessage.setText("Неправильный логин или пароль");
            controller.loginTextField.setText("");
            controller.passwordTextField.setText("");
        }
    }

    //если из облака прилетает файл, то сохраняем его на локальном репозитории
    private void saveSmallFile(CloudSmallFile message) {
        Path path = Paths.get("client/storage/" + message.getName());
        byte[] content = message.getContent();

        try {
            if (Files.exists(path))
                Files.delete(path);

            Files.write(path, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            controller.drawLocalTableView();
        }
    }

    private void saveLargeFile(CloudLargeFile message) {
        Path path = Paths.get("client/storage/" + message.getName());
        byte[] contentPart = message.getContent();
        try {
            if (message.getPartNumber() == 1) {
                if (Files.exists(path))
                    Files.delete(path);
                Files.write(path, contentPart, StandardOpenOption.CREATE_NEW);
            } else {
                Files.write(path, contentPart, StandardOpenOption.APPEND);
                if (message.getPartNumber() == message.getParts()) {
                    controller.drawLocalTableView();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ElementForTableView> getCloudFilesStorage() {
        return CloudController.getCloudStorage();
    }

    //метод, который отправляет наш файл в облако, методу необходимо сообщить название отправляемого файла
    public void uploadFileOnCloud(String name) {

        int partOfFile = 1024 * 1024 * 20;

        try {
            long size = Files.size(Paths.get("client/storage/" + name));
            if (size <= partOfFile) {
                sendSmallFile(name);
            } else {
                    int parts = (int) Math.ceil((double) (size / partOfFile));
                    sendLargeFile(name, parts);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSmallFile(String name) {
        try {
            byte[] content = Files.readAllBytes(Paths.get("client/storage/" + name));
            CloudSmallFile file = new CloudSmallFile(name, content);
            encoder.writeObject(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLargeFile(String name, int parts) {


        int part = 1;
        int partSizeOfFile = 1024 * 1024 * 20; //20 мегабайт
        int currentPlaceInArray = 0;
        int read;
        byte[] current = new byte[partSizeOfFile];

        try (InputStream fileForeSend = new BufferedInputStream(new FileInputStream("client/storage/" + name))) {

            System.out.println(Files.size(Paths.get("client/storage/" + name)));

            do {
                read = fileForeSend.read();
                if (read != -1) {
                    current[currentPlaceInArray++] = (byte) read;

                    if (currentPlaceInArray == partSizeOfFile) {
                        System.out.println(part + " middle");
                        CloudLargeFile file = new CloudLargeFile(name, current, parts, part++);
                        encoder.writeObject(file);
                        currentPlaceInArray = 0;
                    }
                } else {
                    byte[] lastPart = new byte[currentPlaceInArray];
                    System.arraycopy(current, 0, lastPart, 0, currentPlaceInArray);
                    System.out.println(part + " end");
                    CloudLargeFile file = new CloudLargeFile(name, lastPart, parts, part);
                    encoder.writeObject(file);
                }
            } while (read != -1);

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

    public void deleteFileOnCloudStorage(String name) {
        CloudCommand command = new CloudCommand("/deleteFile", name);
        try {
            encoder.writeObject(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAuthorizationMsg(String login, String password, String status) {
        CloudAuthorization authorization = new CloudAuthorization(login, password, status);
        try {
            encoder.writeObject(authorization);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}