package org.example.client;

import org.example.client.Exceptions.ManagerInitializeException;
import org.example.client.Exceptions.SendRecvException;
import org.example.client.Utils.RecvResult;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class ConnectionManager {

    public static final int MAX_BYTES = 1024;

    private static final String CONNECTION_NOT_ESTABLISHED_MSG = "You are not connected to a server!";
    private static final String INCORRECT_IP_MESSAGE = "Given IP address is in incorrect format!";
    private static final String NOT_CONNECTED_MESSAGE = "You are not connected to a server!";

    private Socket clientSocket;

    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private String ip;
    private int port;

    public ConnectionManager(String ip, int port) throws IOException, ManagerInitializeException {
        this.clientSocket = new Socket();

        setPortAndIp(ip, port);
    }

    private void setPortAndIp(String ip, int port) throws IOException, ManagerInitializeException {
        if (ip == null || ip.isEmpty())
            throw new ManagerInitializeException(INCORRECT_IP_MESSAGE);

        this.port = port;
        this.ip = ip;
    }

    private void setupStreams() throws IOException {
        this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(clientSocket.getInputStream());
    }

    public void closeStreams() {
        try {
            outputStream.close();
            inputStream.close();
        } catch (Exception ignored) {
        }
    }

    public void connect() throws IOException, SendRecvException {
        SocketAddress address = new InetSocketAddress(ip, port);
        clientSocket.connect(address);

        setupStreams();
    }

    public void disconnect() throws IOException {
        if (!clientSocket.isConnected())
            throw new IOException(NOT_CONNECTED_MESSAGE);

        clientSocket.close();
        closeStreams();
    }

    public int sendMessage(String message) throws SendRecvException, IOException {
        if (!clientSocket.isConnected())
            throw new SendRecvException(CONNECTION_NOT_ESTABLISHED_MSG);

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

        String recvMessage = new String(bytes, 0, bytesRead);

        return new RecvResult(bytesRead, recvMessage);
    }

    public boolean isConnected() {
        return clientSocket.isConnected();
    }

}
