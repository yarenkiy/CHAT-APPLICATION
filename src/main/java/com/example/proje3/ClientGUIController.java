// ClientGUIController.java
package com.example.proje3;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

public class ClientGUIController implements Initializable {
    @FXML private TextArea chatArea;
    @FXML private TextField messageField;
    @FXML private ListView<String> userListView;
    @FXML private Button sendButton;
    @FXML private Button requestChatButton;
    @FXML private Button leaveChatButton;
    @FXML private VBox rootPane;
    @FXML private ToggleButton themeToggle;

    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        TextInputDialog dialog = new TextInputDialog("User" + System.currentTimeMillis() % 1000);
        dialog.setTitle("write a nickname");
        dialog.setHeaderText("nickname");
        dialog.setContentText("Nickname:");

        Optional<String> result = dialog.showAndWait();
        String username = result.orElse("User" + System.currentTimeMillis() % 1000);

        client = new Client(username, this);
        client.startConnection("localhost", 8888);

        messageField.setDisable(true);
        sendButton.setDisable(true);
        leaveChatButton.setDisable(true);

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        userListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    requestChatButton.setDisable(newValue == null);
                }
        );
    }

    @FXML
    private void handleRequestChat() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            client.requestChat(selectedUser);
            addMessage("Chat request sent to " + selectedUser);
        }
    }

    @FXML
    private void handleLeaveChat() {
        client.leaveChat();
        messageField.setDisable(true);
        sendButton.setDisable(true);
        leaveChatButton.setDisable(true);
    }

    @FXML
    private void handleSend() {
        sendMessage();
    }

    private void sendMessage() {
        if (client != null && messageField != null) {
            String message = messageField.getText().trim();
            if (!message.isEmpty()) {
                client.sendMessage(message);
                messageField.clear();
            }
        }
    }

    public void addMessage(String message) {
        if (chatArea != null) {
            String timestamp = String.format("[%tT] ", new java.util.Date());
            chatArea.appendText(timestamp + message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE);
        }
    }

    public void updateUserList(List<String> users) {
        userListView.getItems().clear();
        userListView.getItems().addAll(users);
    }

    public void showChatRequest(String fromUsername) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Chat Request");
        alert.setHeaderText("Chat Request from " + fromUsername);
        alert.setContentText("Do you want to accept the chat request?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            client.acceptChatRequest(fromUsername);
        } else {
            client.rejectChatRequest(fromUsername);
        }
    }

    public void chatAccepted(String username) {
        messageField.setDisable(false);
        sendButton.setDisable(false);
        leaveChatButton.setDisable(false);
        addMessage("Chat started with " + username);
    }

    public void chatRejected(String username) {
        addMessage("Chat request rejected by " + username);
    }

    public void chatEnded(String username) {
        messageField.setDisable(true);
        sendButton.setDisable(true);
        leaveChatButton.setDisable(true);
        addMessage("Chat ended with " + username);
    }

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
}