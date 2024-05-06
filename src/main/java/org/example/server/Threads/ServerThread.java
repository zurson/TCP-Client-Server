package org.example.server.Threads;

import org.example.server.ClientsListAccess;
import org.example.server.Exceptions.ClientException;
import org.example.server.Exceptions.ListenException;
import org.example.server.ServerApp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerThread extends Thread implements ClientsListAccess {
    private static final String SERVER_PREFIX = "[SERVER] ";
    private static final String PORT_LESS_THAN_ZERO_MSG = "Port cannot be less than zero!";
    private static final String SERVER_SOCKET_NULL_MSG = SERVER_PREFIX + "Server socket cannot be null!";
    private static final String SERVER_OFFLINE_MSG = SERVER_PREFIX + "Server must bo online before accepting connections!";
    private static final String CLOSING_CLIENT_MSG = "Closing client: ";

    private static final int MAX_CONNECTIONS = 1;

    private final List<ClientThread> clients;
    private final ServerSocket serverSocket;
    private final AtomicBoolean running;
    private final Lock lock;


    public ServerThread(String portInText) throws IOException {
        int port = convertPort(portInText);

        this.running = new AtomicBoolean(false);
        this.clients = new ArrayList<>();
        this.lock = new ReentrantLock();

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
                spawnClientThread(clientSocket);
            } catch (IOException | ClientException e) {
                log(e.getMessage());
                closeClientSocket(clientSocket);
            }
        }

    }


    private Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }


    private void spawnClientThread(Socket clientSocket) throws ClientException, IOException {
        ClientThread clientThread = new ClientThread(clientSocket, this, maxClientsReached());
        Thread thread = new Thread(clientThread);
        thread.start();

        lock.lock();
        this.clients.add(clientThread);
        lock.unlock();
    }


    private boolean maxClientsReached() {
        return ServerApp.getController().getConnectionCounter() >= MAX_CONNECTIONS;
    }


    private void disconnectAllClients() {

        for (ClientThread clientThread : clients) {
            log(CLOSING_CLIENT_MSG + clientThread.getIdentifier());
            clientThread.closeSocket();
        }

        this.clients.clear();
    }


    private void closeClientSocket(Socket clientSocket) {
        if (clientSocket == null)
            return;

        try {
            lock.lock();
            clientSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            lock.unlock();
        }

    }


    public void closeServer() {
        if (serverSocket == null || serverSocket.isClosed())
            return;
        ;
        try {
            lock.lock();
            disconnectAllClients();
            serverSocket.close();
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            setRunning(false);
            lock.unlock();
        }
    }


    private void bindSocket(int port) throws IOException {
        SocketAddress socketPort = new InetSocketAddress("0.0.0.0", port);
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


    private void setRunning(boolean running) {
        this.running.set(running);
    }

    @Override
    public void removeClient(ClientThread clientThread) {
        if (clientThread == null)
            return;

        lock.lock();
        this.clients.remove(clientThread);
        ServerApp.getController().updateConnections(getCurrentConnectionsIds());


        lock.unlock();
    }

    private List<String> getCurrentConnectionsIds() {
        List<String> ids = new ArrayList<>();

        for (ClientThread clientThread : clients)
            ids.add(clientThread.getIdentifier());

        return ids;
    }

}
