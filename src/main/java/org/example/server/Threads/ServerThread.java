package org.example.server.Threads;

import org.example.server.Exceptions.ClientException;
import org.example.server.Exceptions.ListenException;
import org.example.server.ServerApp;
import org.example.server.Utils.RecvResult;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerThread extends Thread {
    private static final String SERVER_PREFIX = "[SERVER] ";
    private static final String PORT_LESS_THAN_ZERO_MSG = "Port cannot be less than zero!";
    private static final String SERVER_SOCKET_NULL_MSG = SERVER_PREFIX + "Server socket cannot be null!";
    private static final String SERVER_OFFLINE_MSG = SERVER_PREFIX + "Server must bo online before accepting connections!";

    private static final String NEW_CLIENT_CONNECTED_PREFIX = "New Client connected: ";
    private static final String CLIENT_DISCONNECTED_PREFIX = "Client disconnected: ";
    private static final String CLIENT_NOT_CONNECTED_MSG = "Client is not connected!";


    private final ServerSocket serverSocket;
    private final AtomicBoolean running;


    private final static int MAX_BYTES = 1024;
    private Socket clientSocket;
    private UUID uuid;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    public ServerThread(String portInText) throws IOException {
        int port = convertPort(portInText);

        this.running = new AtomicBoolean(false);

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
        setRunning(true);

        while (running.get()) {
            try {
                clientSocket = acceptConnection();

                initClient(clientSocket);
                recvAndSendMessageToClient();

            } catch (IOException | ClientException e) {
                log(e.getMessage());
                closeClientSocket();
            }
        }

    }


    private Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }


    private void initClient(Socket clientSocket) throws ClientException, IOException {
        this.clientSocket = clientSocket;
        this.uuid = UUID.randomUUID();

        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());

        ServerApp.getController().addConnection(getIdentifier());
        log(NEW_CLIENT_CONNECTED_PREFIX + getIdentifier());
    }


    private void recvAndSendMessageToClient() {
        while (true) {
            try {
                RecvResult recvResult = recvMessage();
                log(String.format(getIdentifier() + " SAYS (%dB): %s", recvResult.getBytes(), recvResult.getMessage()));
                sendMessage(recvResult.getMessage());
            } catch (IOException e) {
                ServerApp.getController().log(CLIENT_DISCONNECTED_PREFIX + getIdentifier());
                closeClientSocket();
                break;
            }

        }
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


    public String getIdentifier() {
        return String.format("%s:%d (%s)", clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort(), uuid);
    }


    public void closeClientSocket() {
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


    public void closeServer() {
        if (serverSocket == null || serverSocket.isClosed())
            return;

        try {
            closeClientSocket();
            serverSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            setRunning(false);
        }
    }


    private void bindSocket(int port) throws IOException {
        SocketAddress socketPort = new InetSocketAddress("0.0.0.0", port);
        serverSocket.bind(socketPort, 1);
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


    private void setRunning(boolean running) {
        this.running.set(running);
    }

}
