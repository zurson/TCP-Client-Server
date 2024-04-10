package org.example.server.Threads;

import org.example.server.Exceptions.ClientException;
import org.example.server.Exceptions.ListenException;
import org.example.server.ServerApp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class ServerThread extends Thread {
    private static final String SERVER_PREFIX = "[SERVER] ";

    private static final String PORT_LESS_THAN_ZERO_MSG = "Port cannot be less than zero!";
    private static final String SERVER_SOCKET_NULL_MSG = SERVER_PREFIX + "Server socket cannot be null!";
    private static final String SERVER_OFFLINE_MSG = SERVER_PREFIX + "Server must bo online before accepting connections!";

    private final ServerSocket serverSocket;
    private boolean online;

    public ServerThread(String portInText) throws IOException, ListenException, IllegalArgumentException {
        int port = convertPort(portInText);

        serverSocket = new ServerSocket();
        bindSocket(port);
    }



    @Override
    public void run() {
        try {
            listenForConnections();
        } catch (ListenException e) {
            log(e.getMessage());
            closeServer();
        }
    }

    private void listenForConnections() throws ListenException {
        if (serverSocket == null)
            throw new ListenException(SERVER_SOCKET_NULL_MSG);

        if (serverSocket.isClosed())
            throw new ListenException(SERVER_OFFLINE_MSG);

        Socket clientSocket = null;
        this.online = true;

        while (online) {
            try {
                clientSocket = acceptConnection();
                runClientThread(clientSocket);
            } catch (IOException | ClientException e) {
                log(e.getMessage());
                closeClientSocket(clientSocket);
            }
        }

    }

    private Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }

    private void runClientThread(Socket clientSocket) throws ClientException {
        ClientThread clientThread = new ClientThread(clientSocket);
        Thread thread = new Thread(clientThread);
        thread.setDaemon(true);
        thread.start();
    }

    private void closeClientSocket(Socket clientSocket) {
        if (clientSocket == null)
            return;

        try {
            clientSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            this.online = false;
        }
    }

    public void closeServer() {
        if (serverSocket == null || serverSocket.isClosed())
            return;

        try {
            serverSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            this.interrupt();
        }
    }

    private void bindSocket(int port) throws IOException {
        SocketAddress socketPort = new InetSocketAddress(port);
        serverSocket.bind(socketPort);
    }

    private int convertPort(String portInText) throws NumberFormatException {
        int port = Integer.parseInt(portInText);

        if (port < 0)
            throw new NumberFormatException(PORT_LESS_THAN_ZERO_MSG);

        return port;
    }

    private void log(String message) {
        ServerApp.getController().log(SERVER_PREFIX + message);
    }


}
