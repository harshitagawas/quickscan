package org.example.quickscan;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainView extends BorderPane {

    private QRGeneratorView generatorView;
    private QRScannerView scannerView;

    public MainView() {
        this.getStyleClass().add("main-view");
        setupUI();
    }

    private void setupUI() {
        // Header
        Label titleLabel = new Label("QuickScan");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.getStyleClass().add("header-title");

        HBox header = new HBox(titleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("header");
        
        setTop(header);

        // Main content - Options
        Button generateButton = new Button("Generate QR Code");
        generateButton.getStyleClass().add("main-button");
        generateButton.setOnAction(e -> showGeneratorView());

        Button scanButton = new Button("Scan QR Code");
        scanButton.getStyleClass().add("main-button");
        scanButton.setOnAction(e -> showScannerView());

        VBox optionsBox = new VBox(20, generateButton, scanButton);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(50));
        
        setCenter(optionsBox);

        // Initialize views
        generatorView = new QRGeneratorView(this);
        scannerView = new QRScannerView(this);
    }

    public void showGeneratorView() {
        setCenter(generatorView);
    }

    public void showScannerView() {
        setCenter(scannerView);
    }

    public void showMainOptions() {
        Button generateButton = new Button("Generate QR Code");
        generateButton.getStyleClass().add("main-button");
        generateButton.setOnAction(e -> showGeneratorView());

        Button scanButton = new Button("Scan QR Code");
        scanButton.getStyleClass().add("main-button");
        scanButton.setOnAction(e -> showScannerView());

        VBox optionsBox = new VBox(20, generateButton, scanButton);
        optionsBox.setAlignment(Pos.CENTER);
        optionsBox.setPadding(new Insets(50));
        
        setCenter(optionsBox);
    }
}