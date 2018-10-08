package com.gb.cloud.controller;

import com.gb.cloud.network.CloudNetwork;
import com.gb.cloud.tableViewElements.ElementForTableView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//Контроллер для управления логикой и представлением
public class CloudController {

    @FXML
    public Button download;
    @FXML
    public Button refreshCloudFile;
    @FXML
    public Button deleteCloudFile;
    @FXML
    public Button refreshLocalFile;
    @FXML
    public Button deleteLocalFile;
    @FXML
    public Button upload;
    @FXML
    private CloudNetwork network;

    @FXML
    public VBox loginPane;
    @FXML
    public VBox cloudPane;
    @FXML
    public VBox registrationPane;

    private Path pathToStorage;

    @FXML
    private TableView<ElementForTableView> localTableView;
    @FXML
    private TableColumn<ElementForTableView, String> localFileName;
    @FXML
    private TableColumn<ElementForTableView, String> localFileSize;
    @FXML
    private TableColumn<ElementForTableView, Date> localFileCreateDate;

    private ObservableList<ElementForTableView> localItems = FXCollections.observableArrayList();

    @FXML
    private TableView<ElementForTableView> cloudTableView;
    @FXML
    private TableColumn<ElementForTableView, String> cloudFileName;
    @FXML
    private TableColumn<ElementForTableView, String> cloudFileSize;
    @FXML
    private TableColumn<ElementForTableView, Date> cloudFileCreateDate;

    private ObservableList<ElementForTableView> cloudItems = FXCollections.observableArrayList();

    private static final int PORT = 8189;
    private static final String SERVER_IP = "localhost";

    @FXML
    public void initialize(){

        localFileName.prefWidthProperty().bind(localTableView.widthProperty().divide(2.6));
        localFileSize.prefWidthProperty().bind(localTableView.widthProperty().divide(4.2));
        localFileCreateDate.prefWidthProperty().bind(localTableView.widthProperty().divide(2.7));

        cloudFileName.prefWidthProperty().bind(cloudTableView.widthProperty().divide(2.6));
        cloudFileSize.prefWidthProperty().bind(cloudTableView.widthProperty().divide(4.2));
        cloudFileCreateDate.prefWidthProperty().bind(cloudTableView.widthProperty().divide(2.7));

        download.setCursor(Cursor.HAND);
        upload.setCursor(Cursor.HAND);
        deleteCloudFile.setCursor(Cursor.HAND);
        deleteLocalFile.setCursor(Cursor.HAND);
        refreshCloudFile.setCursor(Cursor.HAND);
        refreshLocalFile.setCursor(Cursor.HAND);
    }



    //прорисовываем информацию о хранящихся файлах на локальном репозитории, в TableView
    private void drawLocalTableView() {
        localItems.clear();//сперва очищаем список, чтобы при каждом обновлении списка файлы не дублировались

        //пробигаемся по локальному репозиторию, чтобы собрать информацию о всех файлах
        pathToStorage = Paths.get("client/storage");
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
    private void drawCloudTableView() {
        cloudItems.clear();
        cloudTableView.itemsProperty().setValue(cloudItems);

        cloudItems.addAll(network.getCloudFilesStorage());

        cloudFileName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        cloudFileSize.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getSize()));
        cloudFileCreateDate.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getCreateDate()));
    }

    //действия при нажатии на кнопку зарегистрироваться
    public void goToRegistration(ActionEvent actionEvent) {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registrationPane.setVisible(true);
        registrationPane.setManaged(true);
    }

    //действия при нажатии на кнопку войти
    public void goToCloud(ActionEvent actionEvent) {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        cloudPane.setVisible(true);
        cloudPane.setManaged(true);

        //устанавливаем соединение
        connect();
    }

    //действия при нажатии кнопки назад при отображении регистрационного окна
    public void backToLogin(ActionEvent actionEvent) {
        registrationPane.setVisible(false);
        registrationPane.setManaged(false);
        loginPane.setVisible(true);
        loginPane.setManaged(true);
    }

    //Подключаемся к серверу, если авторизация прошла успешно
    public void connect() {
        network = CloudNetwork.getNetwork();
        network.run(PORT, SERVER_IP, "testLogin");

        //прорисовываем оба TableView
        drawLocalTableView();
        drawCloudTableView();
    }

    //перебор файлов в локальном хранилище
    private void walkToLocalDirectory(Path path) {
        try {
            Files.walkFileTree(path, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    //задать логику при обнаружении директории
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    //когда находим файл, добавляем его в список файлов для отображения
                    putFileInTableView(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //сюда прилетает файл, заносим его список элементов для отображения
    //класс ElementForTableView содержит три поля: название, размер, время создания,
    //создаем список для отображения с объектами этого класса
    private void putFileInTableView(Path file) {
        try {
            String name = file.getFileName().toString();
            String size = String.valueOf(file.toFile().length());
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            Date createDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
            ElementForTableView element = new ElementForTableView(name, size, createDate);
            localItems.add(element);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            drawLocalTableView();//обновляес содержимое TableView локального хранилища
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


    private String getSelectedFileNameLocal(TableView<ElementForTableView> tableView) {

        if (tableView == cloudTableView || tableView.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        ElementForTableView element = tableView.getSelectionModel().getSelectedItem();
        return element.getName();
    }

    private String getSelectedFileNameCloud(TableView<ElementForTableView> tableView) {

        if (tableView == localTableView || tableView.getSelectionModel().getSelectedItem() == null) {
            return null;
        }
        ElementForTableView element = tableView.getSelectionModel().getSelectedItem();
        return element.getName();
    }

    public void updateLocalView(ActionEvent actionEvent) {
        drawLocalTableView();
    }

    public void updateCloudView(ActionEvent actionEvent) {
        drawCloudTableView();
    }
}