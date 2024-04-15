package org.example.server.Threads;

import org.example.server.ClientsListAccess;
import org.example.server.Exceptions.ClientException;
import org.example.server.ServerApp;
import org.example.server.Utils.RecvResult;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ClientThread extends Thread {
    private static final String CLIENT_PREFIX = "[CLIENT] ";
    private static final String SERVER_SOCKET_NULL_MSG = "Client socket cannot be null!";
    private static final String CLIENT_OFFLINE_MSG = "Client socket cannot be closed!";
    private static final String NEW_CLIENT_CONNECTED_PREFIX = "New Client connected: ";
    private static final String CLIENT_DISCONNECTED_PREFIX = "Client disconnected: ";
    private static final String CLIENT_NOT_CONNECTED_MSG = "Client is not connected!";

    private final static int MAX_BYTES = 1024;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private final Socket clientSocket;
    private final ClientsListAccess clientsListAccess;
    private final UUID uuid;

    public ClientThread(Socket clientSocket, ClientsListAccess clientsListAccess) throws ClientException, IOException {
        if (clientSocket == null || clientsListAccess == null)
            throw new ClientException(SERVER_SOCKET_NULL_MSG);

        if (clientSocket.isClosed())
            throw new ClientException(CLIENT_OFFLINE_MSG);

        this.clientSocket = clientSocket;
        this.clientsListAccess = clientsListAccess;
        this.uuid = UUID.randomUUID();

        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());

        ServerApp.getController().addConnection();
    }


    @Override
    public void run() {
        ServerApp.getController().log(NEW_CLIENT_CONNECTED_PREFIX + getIdentifier());

        while (true) {
            try {
                RecvResult recvResult = recvMessage();
                log(String.format(getIdentifier() + " SAYS (%dB): %s", recvResult.getBytes(), recvResult.getMessage()));
                sendMessage(recvResult.getMessage());
            } catch (IOException e) {
                ServerApp.getController().log(CLIENT_DISCONNECTED_PREFIX + getIdentifier());
                closeSocket();
                clientsListAccess.removeClient(this);
                break;
            }

        }

    }


    private void log(String message) {
        ServerApp.getController().log(CLIENT_PREFIX + message);
    }


    public void closeSocket() {
        if (clientSocket == null || clientSocket.isClosed())
            return;

        try {
            this.clientSocket.close();
            this.inputStream.close();
            this.outputStream.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            ServerApp.getController().removeConnection();
        }
    }


    public String getIdentifier() {
        return String.format("%s:%d (%s)", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), uuid);
    }


    public int sendMessage(String message) throws IOException {
        if (!clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        if (message.length() > MAX_BYTES)
            message = message.substring(0, MAX_BYTES);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(messageBytes);
        outputStream.flush();

        return messageBytes.length;
    }


    public RecvResult recvMessage() throws IOException {
        byte[] bytes = new byte[MAX_BYTES];
        int bytesRead = inputStream.read(bytes);

        String recvMessage;
        try {
            recvMessage = new String(bytes, 0, bytesRead);
        } catch (IndexOutOfBoundsException ignored) {
            throw new IOException();
        }

        return new RecvResult(bytesRead, recvMessage);
    }

}
