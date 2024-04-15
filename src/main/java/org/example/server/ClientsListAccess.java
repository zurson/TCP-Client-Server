package org.example.server;

import org.example.server.Threads.ClientThread;

public interface ClientsListAccess {

    void removeClient(ClientThread clientThread);

}
