package org.example.quickscan;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.net.URI;

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

        // Header
        Label headerLabel = new Label("Generate QR Code");
        headerLabel.getStyleClass().add("section-header");

        // Back button
        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> mainView.showMainOptions());

        HBox headerBox = new HBox(10, backButton, headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Input type selection
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

        // Content input
        Label contentLabel = new Label("Enter content:");
        contentInput = new TextArea();
        contentInput.setPromptText("Enter text or URL to generate QR code");
        contentInput.setPrefRowCount(5);
        contentInput.setWrapText(true);
        contentInput.setStyle("-fx-text-fill: black;");

        VBox.setVgrow(contentInput, Priority.ALWAYS);

        // Color pickers
        foregroundColorPicker = new ColorPicker(Color.BLACK);
        backgroundColorPicker = new ColorPicker(Color.WHITE);

        HBox colorPickerBox = new HBox(20, new Label("Foreground Color:"), foregroundColorPicker, new Label("Background Color:"), backgroundColorPicker);
        colorPickerBox.setAlignment(Pos.CENTER);

        // Generate button
        Button generateButton = new Button("Generate QR Code");
        generateButton.getStyleClass().add("action-button");
        generateButton.setOnAction(e -> generateQRCode());

        // Save button
        Button saveButton = new Button("Save QR Code");
        saveButton.getStyleClass().add("action-button");
        saveButton.setOnAction(e -> saveQRCode());

        // Share button
        Button shareButton = new Button("Share QR Code");
        shareButton.getStyleClass().add("action-button");
        shareButton.setOnAction(e -> shareQRCode());

        HBox actionBox = new HBox(20, generateButton, saveButton, shareButton);
        actionBox.setAlignment(Pos.CENTER);

        // QR Code display
        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);

        // Status label
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        // Add all components
        this.getChildren().addAll(
                headerBox,
                new Separator(),
                optionsBox,
                contentLabel,
                contentInput,
                colorPickerBox,
                actionBox,
                qrImageView,
                statusLabel
        );
    }

    private void generateQRCode() {
        String content = contentInput.getText().trim();

        if (content.isEmpty()) {
            statusLabel.setText("Please enter content to generate QR code");
            statusLabel.getStyleClass().add("error-text");
            return;
        }

        // Validate URL if URL option is selected
        RadioButton selectedOption = (RadioButton) inputTypeGroup.getSelectedToggle();
        if (selectedOption.getText().equals("URL") && !content.matches("^(https?|ftp)://.*$")) {
            content = "http://" + content;
        }

        Color foregroundColor = foregroundColorPicker.getValue();
        Color backgroundColor = backgroundColorPicker.getValue();

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage bufferedImage = toBufferedImage(bitMatrix, foregroundColor, backgroundColor);
            Image image = SwingFXUtils.toFXImage(bufferedImage, null);

            qrImageView.setImage(image);
            statusLabel.setText("QR Code generated successfully");
            statusLabel.getStyleClass().removeAll("error-text");
            statusLabel.getStyleClass().add("success-text");

        } catch (WriterException e) {
            statusLabel.setText("Error generating QR code: " + e.getMessage());
            statusLabel.getStyleClass().add("error-text");
        }
    }

    private BufferedImage toBufferedImage(BitMatrix matrix, Color foreground, Color background) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // ✅ Fixed bitwise operation — cast before shifting
        int onColor = ((int)(foreground.getOpacity() * 255) << 24)
                | ((int)(foreground.getRed() * 255) << 16)
                | ((int)(foreground.getGreen() * 255) << 8)
                | (int)(foreground.getBlue() * 255);

        int offColor = ((int)(background.getOpacity() * 255) << 24)
                | ((int)(background.getRed() * 255) << 16)
                | ((int)(background.getGreen() * 255) << 8)
                | (int)(background.getBlue() * 255);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? onColor : offColor);
            }
        }
        return image;
    }

    private void shareQRCode() {
        if (qrImageView.getImage() == null) {
            statusLabel.setText("Generate a QR code first");
            statusLabel.getStyleClass().add("error-text");
            return;
        }

        try {
            File tempFile = File.createTempFile("qrcode", ".png");
            tempFile.deleteOnExit();
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrImageView.getImage(), null);
            ImageIO.write(bufferedImage, "png", tempFile);

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    String command = "mailto:?subject=QR%20Code&body=See%20attached%20QR%20code.&attachment=" + tempFile.getAbsolutePath();
                    desktop.mail(new URI(command));
                }
            }

        } catch (Exception e) {
            statusLabel.setText("Error sharing QR code: " + e.getMessage());
            statusLabel.getStyleClass().add("error-text");
        }
    }

    private void saveQRCode() {
        if (qrImageView.getImage() == null) {
            statusLabel.setText("Generate a QR code first");
            statusLabel.getStyleClass().add("error-text");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save QR Code");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        fileChooser.setInitialFileName("qrcode.png");

        File file = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (file != null) {
            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(qrImageView.getImage(), null);
                ImageIO.write(bufferedImage, "png", file);
                statusLabel.setText("QR Code saved to: " + file.getAbsolutePath());
                statusLabel.getStyleClass().removeAll("error-text");
                statusLabel.getStyleClass().add("success-text");
            } catch (IOException e) {
                statusLabel.setText("Error saving QR code: " + e.getMessage());
                statusLabel.getStyleClass().add("error-text");
            }
        }
    }
}
