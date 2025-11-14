package com.example.tcpclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import chat.User;

public class TcpConnection {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static boolean listening = false;

    public static User currentUser;

    public static int currentUserId;

    public static void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public static Socket getSocket() { return socket; }
    public static ObjectOutputStream getOut() { return out; }
    public static ObjectInputStream getIn() { return in; }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User currentUser) {
        TcpConnection.currentUser = currentUser;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static void setCurrentUserId(int currentUserId) {
        TcpConnection.currentUserId = currentUserId;
    }

    public static boolean isListening() { return listening; }
    public static void startListening() { listening = true; }
    public static void stopListening() {
        listening = false;
    }

    public static void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
