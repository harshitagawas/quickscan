package org.example.quickscan;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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

public class QRGeneratorView extends VBox {

    private final MainView mainView;
    private final ToggleGroup inputTypeGroup;
    private final TextArea contentInput;
    private final ImageView qrImageView;
    private final Label statusLabel;
    private final ColorPicker foregroundColorPicker;
    private final ColorPicker backgroundColorPicker;

    public QRGeneratorView(MainView mainView) {
        this.mainView = mainView;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setAlignment(Pos.TOP_CENTER);
        this.getStyleClass().add("generator-view");

        Label headerLabel = new Label("Generate QR Code");
        headerLabel.getStyleClass().add("section-header");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> mainView.showMainOptions());

        HBox headerBox = new HBox(10, backButton, headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        inputTypeGroup = new ToggleGroup();
        RadioButton textOption = new RadioButton("Text");
        textOption.setToggleGroup(inputTypeGroup);
        textOption.setSelected(true);
        textOption.getStyleClass().add("input-option");

        RadioButton urlOption = new RadioButton("URL");
        urlOption.setToggleGroup(inputTypeGroup);
        urlOption.getStyleClass().add("input-option");

        HBox optionsBox = new HBox(20, textOption, urlOption);
        optionsBox.setAlignment(Pos.CENTER);

        Label contentLabel = new Label("Enter content:");
        contentInput = new TextArea();
        contentInput.setPromptText("Enter text or URL to generate QR code");
        contentInput.setPrefRowCount(5);
        contentInput.setWrapText(true);
        contentInput.setStyle("-fx-text-fill: black;");
        VBox.setVgrow(contentInput, Priority.ALWAYS);

        foregroundColorPicker = new ColorPicker(Color.BLACK);
        backgroundColorPicker = new ColorPicker(Color.WHITE);
        HBox colorPickerBox = new HBox(20,
                new Label("Foreground:"), foregroundColorPicker,
                new Label("Background:"), backgroundColorPicker);
        colorPickerBox.setAlignment(Pos.CENTER);

        // Buttons
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
        copyButton.setOnAction(e -> copyQRCodeToClipboard());

        HBox actionBox = new HBox(20, generateButton, saveButton, shareButton, copyButton);
        actionBox.setAlignment(Pos.CENTER);

        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);

        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        this.getChildren().addAll(
                headerBox, new Separator(),
                optionsBox, contentLabel, contentInput,
                colorPickerBox, actionBox, qrImageView, statusLabel
        );
    }

    private void generateQRCode() {
        String content = contentInput.getText().trim();
        if (content.isEmpty()) {
            showStatus("Please enter content to generate QR code", true);
            return;
        }

        RadioButton selectedOption = (RadioButton) inputTypeGroup.getSelectedToggle();
        if (selectedOption.getText().equals("URL") && !content.matches("^(https?|ftp)://.*$")) {
            content = "http://" + content;
        }

        Color fg = foregroundColorPicker.getValue();
        Color bg = backgroundColorPicker.getValue();

        double contrast = Math.abs(fg.getBrightness() - bg.getBrightness());
        if (contrast < 0.5) {
            showStatus("⚠️ Low contrast! Scanner may fail. Use dark text & light background.", true);
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage img = toBufferedImage(matrix, fg, bg);
            Image fxImage = SwingFXUtils.toFXImage(img, null);

            qrImageView.setImage(fxImage);
            showStatus("QR Code generated successfully ✅", false);

        } catch (WriterException e) {
            showStatus("Error generating QR: " + e.getMessage(), true);
        }
    }

    private BufferedImage toBufferedImage(BitMatrix matrix, Color fg, Color bg) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int onColor = ((int)(fg.getOpacity() * 255) << 24)
                | ((int)(fg.getRed() * 255) << 16)
                | ((int)(fg.getGreen() * 255) << 8)
                | (int)(fg.getBlue() * 255);

        int offColor = ((int)(bg.getOpacity() * 255) << 24)
                | ((int)(bg.getRed() * 255) << 16)
                | ((int)(bg.getGreen() * 255) << 8)
                | (int)(bg.getBlue() * 255);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
            }
        }
        return image;
    }

    private void shareQRCode() {
        if (qrImageView.getImage() == null) {
            showStatus("Generate a QR code first", true);
            return;
        }

        try {
            File tempFile = File.createTempFile("QuickScan_QR_", ".png");
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrImageView.getImage(), null);
            ImageIO.write(bufferedImage, "png", tempFile);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile); // Opens in Photos / default image viewer
                showStatus("QR Code opened in Photos — share from there 📷", false);
            } else {
                showStatus("Desktop not supported on this system", true);
            }

        } catch (IOException e) {
            showStatus("Error opening QR image: " + e.getMessage(), true);
        }
    }

    private void copyQRCodeToClipboard() {
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

        showStatus("QR Code copied to clipboard 📋 — paste anywhere!", false);
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
