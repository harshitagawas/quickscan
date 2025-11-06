package org.example.quickscan;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class HistoryManager {
    
    private static final String HISTORY_FILE_NAME = "quickscan_history.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static HistoryManager instance;
    private String historyFilePath;
    
    private HistoryManager() {
        this.historyFilePath = null;
    }
    
    public static HistoryManager getInstance() {
        if (instance == null) {
            instance = new HistoryManager();
        }
        return instance;
    }
    
    public void ensureHistoryLocationSet(javafx.scene.Node node) {
        if (historyFilePath == null) {
            promptForHistoryLocation(node);
        }
    }
    
    private void promptForHistoryLocation(javafx.scene.Node node) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("History Location");
        alert.setHeaderText("Choose History File Location");
        alert.setContentText("Please select a directory where you want to save the history file.\nThis will be used for all future scans and generations.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select History Directory");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            
            File selectedDirectory = directoryChooser.showDialog(node.getScene().getWindow());
            if (selectedDirectory != null) {
                historyFilePath = Paths.get(selectedDirectory.getAbsolutePath(), HISTORY_FILE_NAME).toString();
                createHistoryFileIfNotExists();
            } else {
                historyFilePath = Paths.get(System.getProperty("user.home"), HISTORY_FILE_NAME).toString();
                createHistoryFileIfNotExists();
            }
        } else {
            historyFilePath = Paths.get(System.getProperty("user.home"), HISTORY_FILE_NAME).toString();
            createHistoryFileIfNotExists();
        }
    }
    
    private void createHistoryFileIfNotExists() {
        try {
            Path path = Paths.get(historyFilePath);
            if (!Files.exists(path)) {
                Files.createFile(path);
                String header = "QUICKSCAN QR HISTORY\n" +
                               "===================\n" +
                               "Format: [Action] Type: Content Type | Content | Foreground | Background | Saved File | Date\n" +
                               "\n";
                Files.write(path, header.getBytes(), StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            System.err.println("Error creating history file: " + e.getMessage());
        }
    }
    
    public void addHistoryEntry(String action, String contentType, String content, 
                               String foregroundColor, String backgroundColor, 
                               String savedFilePath, boolean isEncrypted) {
        if (historyFilePath == null) {
            return;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String encryptedIndicator = isEncrypted ? " (Encrypted)" : "";
            
            String entry = String.format(
                "[%s]%s Type: %s | Content: %s | Foreground: %s | Background: %s | Saved File: %s | Date: %s%n",
                action, encryptedIndicator, contentType, 
                truncateContent(content), foregroundColor, backgroundColor, 
                savedFilePath != null ? savedFilePath : "Not saved", timestamp
            );
            
            Files.write(Paths.get(historyFilePath), entry.getBytes(), 
                       StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                       
        } catch (IOException e) {
            System.err.println("Error writing to history file: " + e.getMessage());
        }
    }
    
    private String truncateContent(String content) {
        if (content == null) return "null";
        if (content.length() > 50) {
            return content.substring(0, 47) + "...";
        }
        return content;
    }
    
    public String getHistoryFilePath() {
        return historyFilePath;
    }
    
    public boolean isHistoryLocationSet() {
        return historyFilePath != null;
    }
}