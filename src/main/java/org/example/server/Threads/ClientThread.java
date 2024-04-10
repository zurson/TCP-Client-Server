package org.example.server.Threads;

import org.example.server.Exceptions.ClientException;
import org.example.server.ServerApp;

import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread {
    private static final String CLIENT_PREFIX = "[CLIENT] ";

    private static final String SERVER_SOCKET_NULL_MSG = "Client socket cannot be null!";
    private static final String CLIENT_OFFLINE_MSG = "Client socket cannot be closed!";

    private final Socket clientSocket;

    public ClientThread(Socket clientSocket) throws ClientException {
        if (clientSocket == null)
            throw new ClientException(SERVER_SOCKET_NULL_MSG);

        if (clientSocket.isClosed())
            throw new ClientException(CLIENT_OFFLINE_MSG);

        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        ServerApp.getController().log("New Client connected: " + clientSocket.getInetAddress());
        System.out.println("Client on");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ServerApp.getController().log("New Client disconnected: " + clientSocket.getInetAddress());
        System.out.println("Client off");
        try {
            clientSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        }
    }

    private void log(String message) {
        ServerApp.getController().log(CLIENT_PREFIX + message);
    }

}
