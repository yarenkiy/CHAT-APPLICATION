// ClientHandler.java
package com.example.proje3;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean isRunning = true;

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            username = in.readLine();
            server.registerClient(username, this);

            String message;
            while (isRunning && (message = in.readLine()) != null) {
                if (message.startsWith("CHAT_REQUEST:")) {
                    String targetUser = message.substring("CHAT_REQUEST:".length());
                    server.handleChatRequest(username, targetUser);
                }
                else if (message.startsWith("CHAT_ACCEPT:")) {
                    String targetUser = message.substring("CHAT_ACCEPT:".length());
                    server.handleChatAccept(username, targetUser);
                }
                else if (message.startsWith("CHAT_REJECT:")) {
                    String targetUser = message.substring("CHAT_REJECT:".length());
                    server.handleChatReject(username, targetUser);
                }
                else if (message.startsWith("LEAVE_CHAT:")) {
                    String partner = message.substring("LEAVE_CHAT:".length());
                    server.handleLeaveChat(username, partner);
                }
                else if (message.startsWith("MESSAGE:")) {
                    String[] parts = message.substring("MESSAGE:".length()).split(":", 2);
                    if (parts.length == 2) {
                        String targetUser = parts[0];
                        String messageContent = parts[1];
                        server.deliverMessage(username, targetUser, messageContent);
                    }
                }
                else if (message.equals("GET_USERS")) {

                }
            }
        } catch (IOException e) {
            if (isRunning) {
                e.printStackTrace();
            }
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void closeConnection() {
        isRunning = false;
        try {
            if (username != null) {
                server.removeClient(username);
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public String getUsername() {
        return username;
    }
}