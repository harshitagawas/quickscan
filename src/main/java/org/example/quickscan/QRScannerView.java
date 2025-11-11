package org.example.quickscan;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;

public class QRScannerView extends VBox {

    private final MainView mainView;
    private final ImageView qrImageView;
    private final VBox resultBox;
    private final Label statusLabel;
    private String currentScanResult;

    public QRScannerView(MainView mainView) {
        this.mainView = mainView;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setAlignment(Pos.TOP_CENTER);
        this.getStyleClass().add("scanner-view");

        // Header
        Label headerLabel = new Label("Scan QR Code");
        headerLabel.getStyleClass().add("section-header");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> mainView.showMainOptions());

        HBox headerBox = new HBox(10, backButton, headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Upload button
        Button uploadButton = new Button("Upload QR Code Image");
        uploadButton.getStyleClass().add("action-button");
        uploadButton.setOnAction(e -> selectAndScanFile());

        // QR display
        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);

        // Enable drag & drop
        setupDragAndDrop();

        // Result area
        Label resultLabel = new Label("Scan Result:");
        resultLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        resultBox = new VBox(10);
        resultBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(resultBox, Priority.ALWAYS);

        // Status
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");

        this.getChildren().addAll(
                headerBox,
                new Separator(),
                uploadButton,
                qrImageView,
                resultLabel,
                resultBox,
                statusLabel
        );
    }

    /** ---------- FILE UPLOAD ---------- */
    private void selectAndScanFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select QR Code Image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(this.getScene().getWindow());
        if (file != null) scanQRFromFile(file);
    }

    /** ---------- DRAG & DROP SUPPORT ---------- */
    private void setupDragAndDrop() {
        this.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().get(0);
                if (file.exists()) {
                    scanQRFromFile(file);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    /** ---------- MAIN QR SCAN LOGIC ---------- */
    private void scanQRFromFile(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            qrImageView.setImage(image);

            BufferedImage bufferedImage = ImageIO.read(file);
            String result = decodeQRCode(bufferedImage);

            resultBox.getChildren().clear();
            currentScanResult = result;

            if (result == null) {
                showError("No QR Code found in the image");
                return;
            }

            // Handle encryption
            if (result.startsWith("ENCRYPTED:")) {
                handleEncryptedQR(result.substring(10));
                return;
            }

            displayResult(result);

            // Save history (offline)
            HistoryManager.getInstance().ensureHistoryLocationSet(this);
            HistoryManager.getInstance().addHistoryEntry(
                    "Scanned",
                    determineContentType(result),
                    result,
                    false
            );

        } catch (IOException e) {
            showError("Error reading image: " + e.getMessage());
        }
    }

    private String decodeQRCode(BufferedImage bufferedImage) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            showError("Error decoding QR: " + e.getMessage());
            return null;
        }
    }

    /** ---------- DISPLAY HELPERS ---------- */
    private void displayResult(String result) {
        VBox resultContainer = new VBox(10);
        resultContainer.setAlignment(Pos.CENTER);

        if (result.matches("^(https?|ftp)://.*$")) {
            Hyperlink link = new Hyperlink(result);
            link.setStyle("-fx-text-fill: #1e88e5; -fx-font-size: 15px; -fx-font-weight: bold;");
            link.setOnAction(e -> {
                try {
                    Desktop.getDesktop().browse(new URI(result));
                } catch (Exception ex) {
                    showError("Error opening link: " + ex.getMessage());
                }
            });
            resultContainer.getChildren().add(link);
        } else {
            TextArea text = new TextArea(result);
            text.setEditable(false);
            text.setWrapText(true);
            text.setPrefRowCount(5);
            text.setFont(Font.font("Arial", 15));
            text.setStyle("-fx-text-fill: black; -fx-control-inner-background: #f9f9f9; -fx-border-color: #ccc;");
            resultContainer.getChildren().add(text);
        }

        Button copyButton = new Button("Copy QR Content");
        copyButton.getStyleClass().add("copy-button");
        copyButton.setOnAction(e -> copyToClipboard());
        resultContainer.getChildren().add(copyButton);

        resultBox.getChildren().add(resultContainer);
        showSuccess("QR Code scanned successfully ✅");
    }

    private void copyToClipboard() {
        if (currentScanResult == null || currentScanResult.isEmpty()) {
            showError("No content to copy");
            return;
        }

        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(currentScanResult);
        clipboard.setContent(content);

        showSuccess("Content copied to clipboard ✅");
    }

    private String determineContentType(String content) {
        if (content == null) return "Unknown";
        if (content.matches("^(https?|ftp)://.*$")) return "URL";
        if (content.contains("@") && content.contains(".")) return "Email";
        if (content.matches("^\\d+$")) return "Number";
        return "Text";
    }

    /** ---------- PASSWORD PROTECTED QR ---------- */
    private void handleEncryptedQR(String encryptedContent) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Password Protected QR");
        dialog.setHeaderText("This QR code is password protected.");
        dialog.setContentText("Please enter the password to decrypt:");

        ButtonType decryptType = new ButtonType("Decrypt", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(decryptType, ButtonType.CANCEL);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        javafx.scene.Node decryptButton = dialog.getDialogPane().lookupButton(decryptType);
        decryptButton.setDisable(true);

        passwordField.textProperty().addListener((obs, oldVal, newVal) ->
                decryptButton.setDisable(newVal.trim().isEmpty()));

        dialog.getDialogPane().setContent(passwordField);
        javafx.application.Platform.runLater(passwordField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == decryptType) return passwordField.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(password -> {
            try {
                String decrypted = EncryptionUtil.decrypt(encryptedContent, password);
                resultBox.getChildren().clear();
                currentScanResult = decrypted;
                displayResult(decrypted);
                showSuccess("QR Code decrypted successfully ✅");

                // Save decrypted entry in history
                HistoryManager.getInstance().ensureHistoryLocationSet(this);
                HistoryManager.getInstance().addHistoryEntry(
                        "Scanned (Decrypted)",
                        determineContentType(decrypted),
                        decrypted,
                        true
                );

            } catch (Exception e) {
                showError("Invalid password or decryption error ❌");
            }
        });
    }

    /** ---------- STATUS HELPERS ---------- */
    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("success-text");
        statusLabel.getStyleClass().add("error-text");
    }

    private void showSuccess(String msg) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().removeAll("error-text");
        statusLabel.getStyleClass().add("success-text");
    }
}
