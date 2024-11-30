// Server.java
package com.example.proje3;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Platform;

public class Server {
    private static final int PORT = 8888;
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clientsByUsername;
    private Map<String, Set<String>> activeChats;
    private ServerGUIController guiController;

    public Server(ServerGUIController controller) {
        this.guiController = controller;
        this.clientsByUsername = new HashMap<>();
        this.activeChats = new HashMap<>();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            Platform.runLater(() -> guiController.addLog("Server started on port " + PORT));

            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        new Thread(clientHandler).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void registerClient(String username, ClientHandler handler) {
        clientsByUsername.put(username, handler);
        broadcastUserList();
        Platform.runLater(() -> {
            guiController.addLog("New client connected: " + username);
            guiController.updateClientList(new ArrayList<>(clientsByUsername.keySet()));
        });
    }

    public synchronized void removeClient(String username) {
        clientsByUsername.remove(username);
        // Remove from active chats
        activeChats.values().removeIf(chat -> chat.contains(username));
        broadcastUserList();
        Platform.runLater(() -> {
            guiController.addLog("Client disconnected: " + username);
            guiController.updateClientList(new ArrayList<>(clientsByUsername.keySet()));
        });
    }

    private void broadcastUserList() {
        String userList = "USER_LIST:" + String.join(",", clientsByUsername.keySet());
        for (ClientHandler client : clientsByUsername.values()) {
            client.sendMessage(userList);
        }
    }

    public void handleChatRequest(String fromUsername, String toUsername) {
        ClientHandler targetClient = clientsByUsername.get(toUsername);
        if (targetClient != null) {
            targetClient.sendMessage("CHAT_REQUEST:" + fromUsername);
            Platform.runLater(() ->
                    guiController.addLog("Chat request: " + fromUsername + " -> " + toUsername));
        }
    }

    public void handleChatAccept(String fromUsername, String toUsername) {
        ClientHandler fromClient = clientsByUsername.get(fromUsername);
        ClientHandler toClient = clientsByUsername.get(toUsername);

        if (fromClient != null && toClient != null) {
            String chatId = getChatId(fromUsername, toUsername);
            Set<String> participants = new HashSet<>();
            participants.add(fromUsername);
            participants.add(toUsername);
            activeChats.put(chatId, participants);

            fromClient.sendMessage("CHAT_ACCEPTED:" + toUsername);
            toClient.sendMessage("CHAT_ACCEPTED:" + fromUsername);

            Platform.runLater(() ->
                    guiController.addLog("Chat started: " + fromUsername + " - " + toUsername));
        }
    }

    public void handleChatReject(String fromUsername, String toUsername) {
        ClientHandler toClient = clientsByUsername.get(toUsername);
        if (toClient != null) {
            toClient.sendMessage("CHAT_REJECTED:" + fromUsername);
            Platform.runLater(() ->
                    guiController.addLog("Chat rejected: " + fromUsername + " -> " + toUsername));
        }
    }

    public void handleLeaveChat(String username, String partnerUsername) {
        String chatId = getChatId(username, partnerUsername);
        activeChats.remove(chatId);

        ClientHandler partner = clientsByUsername.get(partnerUsername);
        if (partner != null) {
            partner.sendMessage("CHAT_ENDED:" + username);
        }

        Platform.runLater(() ->
                guiController.addLog("Chat ended: " + username + " - " + partnerUsername));
    }

    public void deliverMessage(String fromUsername, String toUsername, String message) {
        String chatId = getChatId(fromUsername, toUsername);
        if (activeChats.containsKey(chatId)) {
            ClientHandler toClient = clientsByUsername.get(toUsername);
            if (toClient != null) {
                toClient.sendMessage("MESSAGE:" + fromUsername + ": " + message);
                Platform.runLater(() ->
                        guiController.addLog("Message from " + fromUsername + " to " + toUsername + ": " + message));
            }
        }
    }

    private String getChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ?
                user1 + "-" + user2 : user2 + "-" + user1;
    }

    public void stopServer() {
        try {
            for (ClientHandler client : new ArrayList<>(clientsByUsername.values())) {
                client.closeConnection();
            }
            clientsByUsername.clear();
            activeChats.clear();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}