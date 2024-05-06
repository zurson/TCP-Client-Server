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
import java.util.ResourceBundle;

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
    private TextArea textArea, connectionsTextArea;

    private ServerManagerThread serverManager;

    private int connections;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setServerStatus(ServerStatus.OFFLINE);
        setButtonEnable(startButton, true);
        setButtonEnable(stopButton, false);

        textArea.setEditable(false);
        textArea.setWrapText(true);
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

            connections = 0;
            updateConnectionsLabel(connections);

            runServerThread();

            log(SERVER_STARTED_MSG);

            setButtonEnable(stopButton, true);
            setButtonEnable(startButton, false);
            setServerStatus(ServerStatus.ONLINE);

            connectionsTextArea.clear();

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

        serverManager.stopServer();

        log(SERVER_STOPPED_MSG);

        setButtonEnable(stopButton, false);
        setButtonEnable(startButton, true);
        setServerStatus(ServerStatus.OFFLINE);

        connectionsTextArea.clear();

        serverManager = null;

    }


    private void setButtonEnable(Button button, boolean status) {
        if (button == null)
            return;

        Platform.runLater(() -> button.setDisable(!status));
    }

    public void log(String text) {
        if (text == null || text.isEmpty())
            return;

        Platform.runLater(() -> textArea.appendText(text + '\n'));
    }

    public void setServerStatus(ServerStatus status) {
        statusLabel.setText(STATUS_PREFIX + status);
    }

    public void updateConnectionsLabel(int connections) {
        Platform.runLater(() -> connectionsLabel.setText(CONNECTIONS_PREFIX + connections));
    }

    public void addConnection(String name) {
        connections++;

        updateConnectionsLabel(connections);
        connectionsTextArea.setText(name);
    }

    public void removeConnection() {
        connections--;

        updateConnectionsLabel(connections);
        connectionsTextArea.clear();
    }

}