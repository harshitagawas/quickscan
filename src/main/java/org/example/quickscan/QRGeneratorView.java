package org.example.quickscan;

import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class QRGeneratorView extends VBox {

    private final MainView mainView;
    private final ToggleGroup inputTypeGroup;
    private final TextArea contentInput;
    private final ImageView qrImageView;
    private final Label statusLabel;
    private final ColorPicker foregroundColorPicker;
    private final ColorPicker backgroundColorPicker;
    private final CheckBox passwordProtectionCheckBox;
    private final TextField passwordField;
    private String currentContent;

    public QRGeneratorView(MainView mainView) {
        this.mainView = mainView;

        setSpacing(20);
        setPadding(new Insets(30));
        setAlignment(Pos.TOP_CENTER);
        getStyleClass().add("generator-view");

        // Header Section
        Label headerLabel = new Label("Generate QR Code");
        headerLabel.getStyleClass().add("section-header");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> mainView.showMainOptions());

        HBox headerBox = new HBox(10, backButton, headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Input type toggle
        inputTypeGroup = new ToggleGroup();
        RadioButton textOption = new RadioButton("Text");
        textOption.setToggleGroup(inputTypeGroup);
        textOption.setSelected(true);
        textOption.getStyleClass().add("input-option");

        RadioButton urlOption = new RadioButton("URL");
        urlOption.setToggleGroup(inputTypeGroup);
        urlOption.getStyleClass().add("input-option");

        HBox typeBox = new HBox(20, textOption, urlOption);
        typeBox.setAlignment(Pos.CENTER);

        // Content Input Area
        Label contentLabel = new Label("Enter content:");
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        contentInput = new TextArea();
        contentInput.setPromptText("Enter text or URL to generate QR code");
        contentInput.setPrefRowCount(5);
        contentInput.setWrapText(true);
        contentInput.setStyle("-fx-text-fill: black;");
        VBox.setVgrow(contentInput, Priority.ALWAYS);

        // Color pickers
        foregroundColorPicker = new ColorPicker(Color.BLACK);
        backgroundColorPicker = new ColorPicker(Color.WHITE);
        foregroundColorPicker.setPrefHeight(35);
        backgroundColorPicker.setPrefHeight(35);

        Label fgLabel = new Label("Foreground:");
        fgLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label bgLabel = new Label("Background:");
        bgLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        HBox colorBox = new HBox(20, fgLabel, foregroundColorPicker, bgLabel, backgroundColorPicker);
        colorBox.setAlignment(Pos.CENTER);

        // Password Protection
        passwordField = new TextField();
        passwordField.setPromptText("Enter password (optional)");
        passwordField.setVisible(false);
        passwordField.setManaged(false);
        passwordField.setStyle("-fx-text-fill: black;");

        passwordProtectionCheckBox = new CheckBox("Password Protection");
        passwordProtectionCheckBox.getStyleClass().add("input-option");
        passwordProtectionCheckBox.setOnAction(e -> {
            boolean selected = passwordProtectionCheckBox.isSelected();
            passwordField.setVisible(selected);
            passwordField.setManaged(selected);
        });

        HBox passwordBox = new HBox(10, passwordProtectionCheckBox, passwordField);
        passwordBox.setAlignment(Pos.CENTER);

        // Action Buttons
        Button generateButton = new Button("Generate QR");
        generateButton.getStyleClass().add("action-button");
        generateButton.setOnAction(e -> generateQRCode());

        Button saveButton = new Button("Save QR");
        saveButton.getStyleClass().add("action-button");
        saveButton.setOnAction(e -> saveQRCode());

        Button shareButton = new Button("Share QR");
        shareButton.getStyleClass().add("action-button");
        shareButton.setOnAction(e -> shareQRCode());

        Button copyButton = new Button("Copy QR");
        copyButton.getStyleClass().add("action-button");
        copyButton.setOnAction(e -> copyQRCode());

        HBox actionBox = new HBox(20, generateButton, saveButton, shareButton, copyButton);
        actionBox.setAlignment(Pos.CENTER);

        // QR Preview
        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);

        // Status Label
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        // Add all nodes
        getChildren().addAll(
                headerBox, new Separator(),
                typeBox, contentLabel, contentInput,
                colorBox, passwordBox, actionBox,
                qrImageView, statusLabel
        );
    }

    private void generateQRCode() {
        String text = contentInput.getText().trim();
        if (text.isEmpty()) {
            showStatus("Please enter content to generate QR code", true);
            return;
        }

        RadioButton selected = (RadioButton) inputTypeGroup.getSelectedToggle();
        String type = selected != null ? selected.getText() : "Text";
        if (type.equals("URL") && !text.matches("^(https?|ftp)://.*$")) {
            text = "http://" + text;
        }

        currentContent = text;
        boolean encrypted = false;

        // Encryption Handling
        if (passwordProtectionCheckBox.isSelected()) {
            String password = passwordField.getText().trim();
            if (password.isEmpty()) {
                showStatus("Please enter a password for protection", true);
                return;
            }
            try {
                String encryptedPayload = EncryptionUtil.encrypt(text, password);
                text = "ENCRYPTED:" + encryptedPayload;
                encrypted = true;
                showStatus("Content encrypted successfully ✅", false);
            } catch (Exception ex) {
                showStatus("Encryption failed: " + ex.getMessage(), true);
                return;
            }
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints);

            Color fg = foregroundColorPicker.getValue();
            Color bg = backgroundColorPicker.getValue();

            BufferedImage qrImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 300; x++) {
                for (int y = 0; y < 300; y++) {
                    qrImage.setRGB(x, y, matrix.get(x, y)
                            ? fxToRgb(fg)
                            : fxToRgb(bg));
                }
            }

            Image fxImg = SwingFXUtils.toFXImage(qrImage, null);
            qrImageView.setImage(fxImg);
            showStatus("QR Code generated successfully ✅", false);

        } catch (Exception ex) {
            showStatus("Error generating QR: " + ex.getMessage(), true);
        }
    }

    private int fxToRgb(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return (r << 16) | (g << 8) | b;
    }

    private void copyQRCode() {
        if (qrImageView.getImage() == null) {
            showStatus("Generate a QR code first", true);
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        Image snapshot = qrImageView.snapshot(params, null);
        content.putImage(snapshot);
        clipboard.setContent(content);
        showStatus("QR Code copied to clipboard 📋", false);
    }

    private void shareQRCode() {
        if (qrImageView.getImage() == null) {
            showStatus("Generate a QR code first", true);
            return;
        }
        try {
            File temp = File.createTempFile("QuickScan_QR_", ".png");
            BufferedImage img = SwingFXUtils.fromFXImage(qrImageView.getImage(), null);
            ImageIO.write(img, "png", temp);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(temp);
                showStatus("QR Code opened in Photos — share from there 📷", false);
            } else {
                showStatus("Desktop not supported on this system", true);
            }

        } catch (IOException e) {
            showStatus("Error opening QR image: " + e.getMessage(), true);
        }
    }

    private void saveQRCode() {
        if (qrImageView.getImage() == null) {
            showStatus("Generate a QR code first", true);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save QR Code");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName("qrcode.png");

        File file = chooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrImageView.getImage(), null);
                ImageIO.write(bufferedImage, "png", file);
                showStatus("QR Code saved: " + file.getAbsolutePath(), false);
            } catch (IOException e) {
                showStatus("Error saving: " + e.getMessage(), true);
            }
        }
    }

    private void showStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("error-text", "success-text");
        statusLabel.getStyleClass().add(error ? "error-text" : "success-text");
    }
}
