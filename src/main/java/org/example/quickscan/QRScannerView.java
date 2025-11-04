package org.example.quickscan;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class QRScannerView extends VBox {

    private final MainView mainView;
    private final ImageView qrImageView;
    private final TextArea resultText;
    private final Label statusLabel;

    public QRScannerView(MainView mainView) {
        this.mainView = mainView;
        this.setSpacing(20);
        this.setPadding(new Insets(30));
        this.setAlignment(Pos.TOP_CENTER);
        this.getStyleClass().add("scanner-view");

        // Header
        Label headerLabel = new Label("Scan QR Code");
        headerLabel.getStyleClass().add("section-header");
        
        // Back button
        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> mainView.showMainOptions());
        
        HBox headerBox = new HBox(10, backButton, headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Upload button
        Button uploadButton = new Button("Upload QR Code Image");
        uploadButton.getStyleClass().add("action-button");
        uploadButton.setOnAction(e -> uploadQRImage());
        
        // QR Code display
        qrImageView = new ImageView();
        qrImageView.setFitWidth(250);
        qrImageView.setFitHeight(250);
        qrImageView.setPreserveRatio(true);
        
        // Result area
        Label resultLabel = new Label("Scan Result:");
        resultText = new TextArea();
        resultText.setEditable(false);
        resultText.setWrapText(true);
        resultText.setPrefRowCount(5);
        resultText.setStyle("-fx-text-fill: black;");
        resultText.setStyle("-fx-font-size: 16px; -fx-text-fill: black;");



        VBox.setVgrow(resultText, Priority.ALWAYS);
        
        // Status label
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("status-label");
        
        // Add all components
        this.getChildren().addAll(
                headerBox,
                new Separator(),
                uploadButton,
                qrImageView,
                resultLabel,
                resultText,
                statusLabel
        );
    }
    
    private void uploadQRImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select QR Code Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(this.getScene().getWindow());
        if (file != null) {
            try {
                // Display the image
                Image image = new Image(file.toURI().toString());
                qrImageView.setImage(image);
                
                // Process the image to scan QR code
                BufferedImage bufferedImage = ImageIO.read(file);
                String result = decodeQRCode(bufferedImage);
                
                if (result != null) {
                    resultText.setText(result);
                    statusLabel.setText("QR Code scanned successfully");
                    statusLabel.getStyleClass().removeAll("error-text");
                    statusLabel.getStyleClass().add("success-text");
                } else {
                    resultText.setText("");
                    statusLabel.setText("No QR Code found in the image");
                    statusLabel.getStyleClass().add("error-text");
                }
                
            } catch (IOException e) {
                statusLabel.setText("Error reading image: " + e.getMessage());
                statusLabel.getStyleClass().add("error-text");
            }
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
            return null; // No QR code found in the image
        } catch (Exception e) {
            statusLabel.setText("Error decoding QR code: " + e.getMessage());
            statusLabel.getStyleClass().add("error-text");
            return null;
        }
    }
}