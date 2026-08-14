package com.reminder.desktop;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleInstanceManager {
    private static final int PORT = 49831;
    private static ServerSocket serverSocket;
    private static final String ACTIVATE_CMD = "ACTIVATE";
    private static final String ACK_RESP = "ACK";

    public static boolean checkAndRegister() {
        try {
            // Try to bind to localhost port 49831
            serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
            
            // Start listener thread to listen for subsequent launch attempts
            Thread thread = new Thread(() -> {
                while (serverSocket != null && !serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                        
                        String message = in.readLine();
                        if (ACTIVATE_CMD.equals(message)) {
                            out.println(ACK_RESP);
                            // Notify application to show window
                            Platform.runLater(() -> {
                                if (MainApplication.getInstance() != null) {
                                    MainApplication.getInstance().showAndFocus();
                                }
                            });
                        }
                    } catch (IOException e) {
                        // socket closed or error in handling connection
                    }
                }
            }, "SingleInstanceListener");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (IOException e) {
            // Port already in use - try to communicate with the existing instance
            try (Socket socket = new Socket("127.0.0.1", PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                out.println(ACTIVATE_CMD);
                String response = in.readLine();
                if (ACK_RESP.equals(response)) {
                    System.out.println("Reminder Desktop is already running. Activating existing window.");
                } else {
                    System.err.println("Port " + PORT + " is occupied by an unrelated application. Exiting due to conflict.");
                }
            } catch (IOException ex) {
                System.err.println("Port " + PORT + " is occupied by an unresponsive application. Exiting.");
            }
            return false;
        }
    }
    
    public static void shutdown() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }
    }
}
