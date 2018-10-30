package com.gb.cloud;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static final String FXML = "/fxml/cloud.fxml";
    private static final String TITLE = "Cloud service";
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    private static Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource(FXML));
        primaryStage.setTitle(TITLE);
        Scene scene = new Scene(root, Main.WIDTH, Main.HEIGHT);
        primaryStage.setMinHeight(600.0);
        primaryStage.setMinWidth(600.0);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Stage getStage() {
        return stage;
    }
}
