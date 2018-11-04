package com.gb.cloud.network;

import com.gb.cloud.controller.Controller;
import com.gb.cloud.message.*;
import com.gb.cloud.tableViewElements.ElementForTableView;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

//Класс в котором содержится бизнес-логика приложения
public class Network {

    private static final int PORT = 8189;
    private static final String SERVER_IP = "localhost";

    private static volatile Network network;
    private static Controller controller;

    private static Socket socket;

    private ObjectEncoderOutputStream encoder;
    private ObjectDecoderInputStream decoder;

    private ArrayBlockingQueue<Object> incomingData = new ArrayBlockingQueue<>(30);

    private Network() {

    }

    public static Network getNetwork(Controller connectController) {
        if (network == null) {
            synchronized (Network.class) {
                if (network == null) {
                    network = new Network();
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
                        Object message = decoder.readObject();
                        incomingData.put(message);

                        //если с сервера прилетает список файлов, обновляем список на клиенте
                        if (message instanceof CloudFilesList) {
                            CloudFilesList msg = (CloudFilesList) incomingData.poll();
                            if (msg == null) {
                                return;
                            }
                            Platform.runLater(() -> Controller.setCloudStorage(msg.getServerStorageList()));
                            controller.drawCloudTableView();

                        }
                        if (message instanceof CloudCommand) {
                            CloudCommand msg = (CloudCommand) incomingData.poll();
                            if (msg == null) {
                                return;
                            }
                            doCommand(msg);
                        }
                        if (message instanceof CloudFile) {
                            CloudFile msg = (CloudFile) incomingData.poll();
                            if (msg == null) {
                                return;
                            }
                            saveFile(msg);
                        }
                    }
                } catch (IOException | ClassNotFoundException | InterruptedException e) {
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

    private void saveFile(CloudFile message) {
        Path path = Paths.get("client/storage/" + message.getName());
        byte[] contentPart = message.getContent();
        try {
            if (message.getPartNumber() == 1) {
                if (Files.exists(path))
                    Files.delete(path);
                Files.write(path, contentPart, StandardOpenOption.CREATE_NEW);
            } else {
                Files.write(path, contentPart, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (message.getPartNumber() % 100 == 0) {
                controller.drawLocalTableView();
            }
            if (message.getPartNumber() == message.getParts()) {
                controller.drawLocalTableView();
                Platform.runLater(() -> controller.cloudIndicator.setVisible(false));
            }
        }
    }

    public ArrayList<ElementForTableView> getCloudFilesStorage() {
        return Controller.getCloudStorage();
    }

    //метод, который отправляет наш файл в облако, методу необходимо сообщить название отправляемого файла
    public void uploadFileOnCloud(String name) {

        Platform.runLater(() -> controller.localIndicator.setVisible(true));

        try {
            long size = Files.size(Paths.get("client/storage/" + name));
            int partOfFile = 1024 * 1024 * 20;
            int parts = (int) Math.ceil((double) size / (double) partOfFile);

            Thread fileSendThread = new Thread(() -> sendFile(name, parts));
            fileSendThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String name, int parts) {

        int part = 1;
        int partSizeOfFile = 1024 * 1024 * 20; //20 мегабайт
        int read;
        byte[] current = new byte[partSizeOfFile];

        try (InputStream fileForeSend = new BufferedInputStream(new FileInputStream("client/storage/" + name))) {

            do {

                read = fileForeSend.read(current);
                if (read != -1) {

                    if (read == partSizeOfFile) {
                        System.out.println(part + " middle");
                        CloudFile file = new CloudFile(name, current, parts, part++);
                        encoder.writeObject(file);
                    } else {
                        byte[] lastPart = Arrays.copyOfRange(current, 0, read);
                        System.out.println(part + " end");
                        CloudFile file = new CloudFile(name, lastPart, parts, part);
                        encoder.writeObject(file);
                        Platform.runLater(() -> controller.localIndicator.setVisible(false));
                    }
                }
            } while (read != -1);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void downloadFileFromCloud(String name) {
        Platform.runLater(() -> controller.cloudIndicator.setVisible(true));
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