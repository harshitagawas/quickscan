package org.example.quickscan;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class QuickScanApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        
        Scene scene = new Scene(mainView, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        
        primaryStage.setTitle("QuickScan - QR Code Generator & Scanner");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}