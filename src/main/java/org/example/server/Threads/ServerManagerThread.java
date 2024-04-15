package org.example.server.Threads;

import org.example.server.Exceptions.ListenException;
import org.example.server.ServerApp;

import java.io.IOException;

public class ServerManagerThread extends Thread {

    private static final String SERVER_THREAD_IS_NULL_RESTART_REQUIRED_MSG = "Server thread is null! Restart the server!";

    private ServerThread serverThread;
    private Thread server;


    public ServerManagerThread(String port) throws ListenException, IOException {
        serverThread = new ServerThread(port);
    }


    @Override
    public void run() {
        if (serverThread == null) {
            ServerApp.getController().log(SERVER_THREAD_IS_NULL_RESTART_REQUIRED_MSG);
            return;
        }

        server = new Thread(serverThread);
        server.start();
        try {
            server.join();
        } catch (InterruptedException e) {
            ServerApp.getController().log(e.getMessage());
        }
    }


    public void stopServer() {
        serverThread.closeServer();
    }

}
