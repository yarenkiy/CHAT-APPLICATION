package com.example.proje3;


import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

public class ServerGUIController implements Initializable {

    @FXML
    private ListView<String> clientListView;
    @FXML
    private TextArea logArea;

    private Server server;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        server = new Server(this);
        server.startServer();
        addLog("Server started...");
    }

    public void updateClientList(List<String> clients) {
        clientListView.getItems().clear();
        clientListView.getItems().addAll(clients);
    }

    public void addLog(String message) {
        logArea.appendText(message + "\n");
    }

    public void shutdown() {
        if (server != null) {
            server.stopServer();
        }
    }
    @FXML
    private VBox rootPane;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void toggleTheme() {
        if (themeToggle.isSelected()) {
            rootPane.getStyleClass().remove("light-mode");
            rootPane.getStyleClass().add("dark-mode");
            themeToggle.setText("Light Mode");
        } else {
            rootPane.getStyleClass().remove("dark-mode");
            rootPane.getStyleClass().add("light-mode");
            themeToggle.setText("Dark Mode");
        }
    }

    @FXML
    public void initialize() {

        rootPane.getStyleClass().add("light-mode");
    }
}