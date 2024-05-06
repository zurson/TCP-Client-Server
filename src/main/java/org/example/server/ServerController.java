package org.example.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.server.Enums.ServerStatus;
import org.example.server.Exceptions.ListenException;
import org.example.server.Threads.ServerManagerThread;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerController implements Initializable {
    private final static String STATUS_PREFIX = "Status: ";
    private final static String CONNECTIONS_PREFIX = "Connections: ";
    private static final String SERVER_IS_NOT_ONLINE_MSG = "Server is not online!";
    private static final String SERVER_STOPPED_MSG = "Server stopped!";
    private static final String SERVER_STARTED_MSG = "Server started!";
    private static final String INCORRECT_PORT_NUMBER_MSG = "Incorrect port number!";


    @FXML
    private Label statusLabel, connectionsLabel;

    @FXML
    private Button startButton, stopButton;

    @FXML
    private TextField portField;

    @FXML
    private TextArea logsTextArea, connectionsTextArea;

    private ServerManagerThread serverManager;

    private int connections;

    private Lock lock;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.lock = new ReentrantLock();

        setServerStatus(ServerStatus.OFFLINE);
        setButtonEnable(startButton, true);
        setButtonEnable(stopButton, false);

        logsTextArea.setEditable(false);
        logsTextArea.setWrapText(true);
    }


    @FXML
    private void onStartButtonClick() {
        startServer();
    }


    @FXML
    private void onStopButtonClick() {
        stopServer();
    }


    private void startServer() {

        try {
            lock.lock();

            connections = 0;
            updateConnectionsLabel(connections);

            runServerThread();

            log(SERVER_STARTED_MSG);

            setButtonEnable(stopButton, true);
            setButtonEnable(startButton, false);
            setServerStatus(ServerStatus.ONLINE);

            clearConnections();

            lock.unlock();

        } catch (IOException | ListenException | IllegalArgumentException e) {

            if (e instanceof IllegalArgumentException)
                log(INCORRECT_PORT_NUMBER_MSG);
            else
                log(e.getMessage());

            serverManager = null;
        }


    }

    private void runServerThread() throws ListenException, IOException {
        serverManager = new ServerManagerThread(portField.getText());
        Thread server = new Thread(serverManager);
        server.start();
    }


    private void stopServer() {
        if (serverManager == null) {
            log(SERVER_IS_NOT_ONLINE_MSG);
            return;
        }

        try {
            lock.lock();

            serverManager.stopServer();

            log(SERVER_STOPPED_MSG);

            setButtonEnable(stopButton, false);
            setButtonEnable(startButton, true);
            setServerStatus(ServerStatus.OFFLINE);

            clearConnections();

            serverManager = null;
        } finally {
            lock.unlock();
        }
    }


    private void setButtonEnable(Button button, boolean status) {
        if (button == null)
            return;

        Platform.runLater(() -> button.setDisable(!status));
    }

    public void log(String text) {
        if (text == null || text.isEmpty())
            return;

        lock.lock();
        Platform.runLater(() -> logsTextArea.appendText(text + '\n'));
        lock.unlock();
    }

    public void setServerStatus(ServerStatus status) {
        statusLabel.setText(STATUS_PREFIX + status);
    }

    public void updateConnectionsLabel(int connections) {
        lock.lock();
        Platform.runLater(() -> connectionsLabel.setText(CONNECTIONS_PREFIX + connections));
        lock.unlock();
    }

    public void increaseConnectionCounter() {
        lock.lock();

        connections++;
        updateConnectionsLabel(connections);

        lock.unlock();
    }

    public void decreaseConnectionCounter() {
        lock.lock();

        connections--;
        updateConnectionsLabel(connections);

        lock.unlock();
    }

    public int getConnectionCounter() {
        try {
            lock.lock();
            return connections;
        } finally {
            lock.unlock();
        }
    }

    public void addConnection(String name) {
        connectionsTextArea.appendText(name + '\n');
    }

    public void updateConnections(List<String> connections) {
        clearConnections();

        for (String conn : connections)
            connectionsTextArea.appendText(conn + '\n');
    }

    private void clearConnections() {
        connectionsTextArea.clear();
    }



}