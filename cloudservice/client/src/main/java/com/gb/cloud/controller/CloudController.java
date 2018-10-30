package com.gb.cloud.controller;

import com.gb.cloud.Main;
import com.gb.cloud.network.CloudNetwork;
import com.gb.cloud.tableViewElements.ElementBuilder;
import com.gb.cloud.tableViewElements.ElementForTableView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CloudController {

    @FXML
    public Button download;
    @FXML
    public Button refreshCloudFile;
    @FXML
    public Button deleteCloudFile;
    @FXML
    public Button deleteLocalFile;
    @FXML
    public Button upload;
    @FXML
    public TextField loginRegisterTextField;
    @FXML
    public PasswordField passwordRegisterTextField;
    @FXML
    public TextField loginTextField;
    @FXML
    public PasswordField passwordTextField;
    @FXML
    public Text serviceMessage;
    @FXML
    public Button fileChooser;
    @FXML
    public Button refreshLocalFile;
    @FXML
    public Button exitFromCloud;

    @FXML
    private CloudNetwork network;

    @FXML
    public VBox loginPane;
    @FXML
    public VBox cloudPane;
    @FXML
    public VBox registrationPane;

    @FXML
    private TableView<ElementForTableView> localTableView;
    @FXML
    private TableColumn<ElementForTableView, String> localFileName;
    @FXML
    private TableColumn<ElementForTableView, String> localFileSize;
    @FXML
    private TableColumn<ElementForTableView, Date> localFileCreateDate;

    private ObservableList<ElementForTableView> localItems = FXCollections.observableArrayList();
    private static ArrayList<ElementForTableView> cloudStorage = new ArrayList<>();

    @FXML
    private TableView<ElementForTableView> cloudTableView;
    @FXML
    private TableColumn<ElementForTableView, String> cloudFileName;
    @FXML
    private TableColumn<ElementForTableView, String> cloudFileSize;
    @FXML
    private TableColumn<ElementForTableView, Date> cloudFileCreateDate;

    private ObservableList<ElementForTableView> cloudItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        initDragAndDropFile();

        //настройка визуальных эффектов для ГПИ
        localFileName.prefWidthProperty().bind(localTableView.widthProperty().divide(2.0));
        localFileSize.prefWidthProperty().bind(localTableView.widthProperty().divide(5.8));
        localFileCreateDate.prefWidthProperty().bind(localTableView.widthProperty().divide(3.1));

        cloudFileName.prefWidthProperty().bind(cloudTableView.widthProperty().divide(2.0));
        cloudFileSize.prefWidthProperty().bind(cloudTableView.widthProperty().divide(5.8));
        cloudFileCreateDate.prefWidthProperty().bind(cloudTableView.widthProperty().divide(3.1));

        download.setCursor(Cursor.HAND);
        upload.setCursor(Cursor.HAND);
        deleteCloudFile.setCursor(Cursor.HAND);
        deleteLocalFile.setCursor(Cursor.HAND);
        refreshCloudFile.setCursor(Cursor.HAND);
        refreshLocalFile.setCursor(Cursor.HAND);
        fileChooser.setCursor(Cursor.HAND);
        exitFromCloud.setCursor(Cursor.HAND);

        //progressBar.progressProperty().setValue(0);
    }

    //////////БЛОК геттеров и сеттеров

    public static ArrayList<ElementForTableView> getCloudStorage() {
        return cloudStorage;
    }

    public static void setCloudStorage(ArrayList<ElementForTableView> cloudStorage) {
        CloudController.cloudStorage = cloudStorage;
    }

    //////////БЛОК обработка кнопок окна приветствия

    //действия при нажатии на кнопку войти
    public void goToCloud(ActionEvent actionEvent) {
        //если поля не заполнены выдать сообщение о необходимости заполнения
        if (loginTextField.getText().trim().equals("")) {
            serviceMessage.setText("Вы не ввели логин");
            return;
        }
        if (passwordTextField.getText().trim().equals("")) {
            serviceMessage.setText("Вы не ввели пароль");
            return;
        }

        connect();
        network.sendAuthorizationMsg(loginTextField.getText(), passwordTextField.getText(), "/enter");
    }

    //действия при нажатии на кнопку зарегистрироваться
    public void goToRegistration(ActionEvent actionEvent) {
        changeScene(loginPane, registrationPane);
        loginTextField.clear();
        passwordTextField.clear();
    }

    //действия при нажатии кнопки назад при отображении регистрационного окна
    public void backToLogin(ActionEvent actionEvent) {
        changeScene(registrationPane, loginPane);
        loginRegisterTextField.clear();
        passwordRegisterTextField.clear();
    }

    private void changeScene(VBox hidePane, VBox showPane) {
        hidePane.setVisible(false);
        hidePane.setManaged(false);
        showPane.setVisible(true);
        showPane.setManaged(true);
        serviceMessage.setText("");
    }

    public void registerInCloud(ActionEvent actionEvent) {
        if (loginRegisterTextField.getText().startsWith(" ")) {
            serviceMessage.setText("Логин не может начинаться с пробела");
            return;
        }
        if (passwordRegisterTextField.getText().startsWith(" ")) {
            serviceMessage.setText("Пароль не может начинаться с пробела");
            return;
        }
        if (loginRegisterTextField.getText().trim().equals("")) {
            serviceMessage.setText("Вы не ввели логин");
            return;
        }
        if (passwordRegisterTextField.getText().trim().equals("")) {
            serviceMessage.setText("Вы не ввели пароль");
            return;
        }
        connect();
        network.sendAuthorizationMsg(loginRegisterTextField.getText(), passwordRegisterTextField.getText(), "/create");
    }


    //////////БЛОК обработка кнопок при работе с файлами

    //метод при помощи, которого отправляем файл в облако
    public void uploadFile(ActionEvent actionEvent) {

        //получаем название файла, в зависимости от того, какой файл в TableView выбран
        String name = getSelectedFileNameLocal(localTableView);
        if (name == null) {
            return;
        }
        network.uploadFileOnCloud(name);
    }

    //при загрузке файла с облака, отправляем команду и название файла
    public void downloadFileFromCloud(ActionEvent actionEvent) {
        String name = getSelectedFileNameCloud(cloudTableView);
        if (name == null) {
            return;
        }
        network.downloadFileFromCloud(name);
    }

    //метод для удаления файлов в локальном хранилище
    public void deleteFileOnLocalStorage(ActionEvent actionEvent) {
        //получаем название файла, в зависимости от того, какой файл в TableView выбран
        String name = getSelectedFileNameLocal(localTableView);
        if (name == null) {
            return;
        }
        //удаляем файл по его названию
        try {
            System.out.println("сейчас будет удален файл " + Paths.get("client/storage/" + name));
            Files.delete(Paths.get("client/storage/" + name));
            drawLocalTableView();//обновляем содержимое TableView локального хранилища
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFileOnCloudStorage(ActionEvent actionEvent) {
        String name = getSelectedFileNameCloud(cloudTableView);
        if (name == null) {
            return;
        }
        network.deleteFileOnCloudStorage(name);
    }


    //////////БЛОК вспомогательные методы

    //Подключаемся к серверу, если авторизация прошла успешно
    private void connect() {
        network = CloudNetwork.getNetwork(this);
        network.run();
    }

    //прорисовываем информацию о хранящихся файлах на локальном репозитории, в TableView
    public void drawLocalTableView() {
        localItems.clear();//сперва очищаем список, чтобы при каждом обновлении списка файлы не дублировались

        //пробигаемся по локальному репозиторию, чтобы собрать информацию о всех файлах
        Path pathToStorage = Paths.get("client/storage");
        walkToLocalDirectory(pathToStorage);

        //заполняем ячейки TableView значениями о файлах
        localTableView.itemsProperty().setValue(localItems);

        //говорим, что именно и в какой ячейке отрисовать
        localFileName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        localFileSize.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getSize()));
        localFileCreateDate.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getCreateDate()));
    }

    //прорисовываем информацию о хранящихся файлах на облаке, в TableView
    //действия аналогичны прорисовки локального хранилища, только мы не пробигаемся по локальной папке,
    //а запрашиваем данные у сервера
    public void drawCloudTableView() {
        cloudItems.clear();
        cloudTableView.itemsProperty().setValue(cloudItems);

        cloudItems.addAll(network.getCloudFilesStorage());

        cloudFileName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        cloudFileSize.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getSize()));
        cloudFileCreateDate.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getCreateDate()));
    }

    //перебор файлов в локальном хранилище
    private void walkToLocalDirectory(Path path) {
        List<ElementForTableView> files = null;
        try {
            files = Files.list(path).map(ElementBuilder::buildElement).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (files != null) {
            localItems.addAll(files);
        }
    }

    public void showCloudWindowAfterLogin() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registrationPane.setVisible(false);
        registrationPane.setManaged(false);
        cloudPane.setVisible(true);
        cloudPane.setManaged(true);
        serviceMessage.setVisible(false);
        serviceMessage.setManaged(false);

    }

    //имя выбранного файла в локальном хранилище
    private String getSelectedFileNameLocal(TableView<ElementForTableView> tableView) {

        if (tableView == cloudTableView || tableView.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        ElementForTableView element = tableView.getSelectionModel().getSelectedItem();
        return element.getName();
    }

    //имя выбранного файла в облачном хранилище
    private String getSelectedFileNameCloud(TableView<ElementForTableView> tableView) {

        if (tableView == localTableView || tableView.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        ElementForTableView element = tableView.getSelectionModel().getSelectedItem();
        return element.getName();
    }

    public void chooseFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        File file = fileChooser.showOpenDialog(Main.getStage());
        if (file == null) {
            return;
        }
        Path src = file.toPath();
        Path dest = Paths.get("client/storage/" + file.getName());
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        drawLocalTableView();
    }

    private void initDragAndDropFile() {
        localTableView.setOnDragOver(event -> {
            if (event.getGestureSource() != localTableView && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        localTableView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    Path pathSrc = Paths.get(file.getAbsolutePath());
                    Path pathDes = Paths.get("client/storage/" + file.getName());
                    try {
                        Files.copy(pathSrc, pathDes, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
            drawLocalTableView();
        });
    }

    public void updateCloudView() {
        drawCloudTableView();
    }

    public void updateLocalView() { drawLocalTableView();
    }

    public void exit() {
        cloudPane.setVisible(false);
        cloudPane.setManaged(false);
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        loginTextField.clear();
        passwordTextField.clear();
        serviceMessage.setVisible(true);
        serviceMessage.setManaged(true);
        network.disconnect();
    }
}
