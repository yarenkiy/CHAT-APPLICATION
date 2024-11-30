// Client.java
package com.example.proje3;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Platform;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ClientGUIController controller;
    private String username;
    private boolean isConnected = false;
    private String currentChatPartner = null;

    public Client(String username, ClientGUIController controller) {
        this.username = username;
        this.controller = controller;
    }

    public void startConnection(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(username);
            isConnected = true;

            new Thread(() -> {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        handleIncomingMessage(message);
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        controller.addMessage("Connection lost: " + e.getMessage());
                    });
                }
            }).start();

            // Request initial user list
            requestUserList();

        } catch (IOException e) {
            Platform.runLater(() -> {
                controller.addMessage("Failed to connect: " + e.getMessage());
            });
        }
    }

    private void handleIncomingMessage(String message) {
        if (message.startsWith("USER_LIST:")) {
            String[] users = message.substring(10).split(",");
            Platform.runLater(() -> controller.updateUserList(Arrays.asList(users)));
        }
        else if (message.startsWith("CHAT_REQUEST:")) {
            String fromUser = message.substring("CHAT_REQUEST:".length());
            Platform.runLater(() -> controller.showChatRequest(fromUser));
        }
        else if (message.startsWith("CHAT_ACCEPTED:")) {
            String acceptedUser = message.substring("CHAT_ACCEPTED:".length());
            currentChatPartner = acceptedUser;
            Platform.runLater(() -> {
                controller.chatAccepted(acceptedUser);
                controller.addMessage("Chat started with " + acceptedUser);
            });
        }
        else if (message.startsWith("CHAT_REJECTED:")) {
            String rejectedUser = message.substring("CHAT_REJECTED:".length());
            currentChatPartner = null;
            Platform.runLater(() -> controller.chatRejected(rejectedUser));
        }
        else if (message.startsWith("CHAT_ENDED:")) {
            String fromUser = message.substring("CHAT_ENDED:".length());
            currentChatPartner = null;
            Platform.runLater(() -> {
                controller.chatEnded(fromUser);
                controller.addMessage("Chat ended with " + fromUser);
            });
        }
        else if (message.startsWith("MESSAGE:")) {
            String actualMessage = message.substring("MESSAGE:".length());
            Platform.runLater(() -> controller.addMessage(actualMessage));
        }
        else {
            Platform.runLater(() -> controller.addMessage(message));
        }
    }

    public void requestChat(String targetUsername) {
        if (isConnected) {
            out.println("CHAT_REQUEST:" + targetUsername);
        }
    }

    public void acceptChatRequest(String fromUsername) {
        if (isConnected) {
            out.println("CHAT_ACCEPT:" + fromUsername);
            currentChatPartner = fromUsername;
        }
    }

    public void rejectChatRequest(String fromUsername) {
        if (isConnected) {
            out.println("CHAT_REJECT:" + fromUsername);
        }
    }

    public void leaveChat() {
        if (isConnected && currentChatPartner != null) {
            out.println("LEAVE_CHAT:" + currentChatPartner);
            currentChatPartner = null;
        }
    }

    public void requestUserList() {
        if (isConnected) {
            out.println("GET_USERS");
        }
    }

    public void sendMessage(String message) {
        if (out != null && isConnected && currentChatPartner != null) {
            out.println("MESSAGE:" + currentChatPartner + ":" + message);
            // Kendi mesajımızı da görüntüle
            Platform.runLater(() -> {
                controller.addMessage("You: " + message);
            });
        } else {
            Platform.runLater(() -> {
                controller.addMessage("Error: Not in an active chat");
            });
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getCurrentChatPartner() {
        return currentChatPartner;
    }

    public void stopConnection() {
        isConnected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}