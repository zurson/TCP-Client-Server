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
import org.example.server.Threads.ServerThread;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    private final static String STATUS_PREFIX = "Status: ";
    private final static String CONNECTIONS_PREFIX = "Connections: ";


    @FXML
    private Label statusLabel, getConnectionsLabel;

    @FXML
    private Button startButton, stopButton;

    @FXML
    private TextField portField;

    @FXML
    private TextArea textArea;

    private Thread server;
    private ServerThread serverThread;

    private int connections;




    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setServerStatus(ServerStatus.OFFLINE);
        setButtonEnable(startButton, true);
        setButtonEnable(stopButton, false);

        connections = 0;
    }

    @FXML
    private void onStartButtonClick() {

        Platform.runLater(() -> {
            try {
                serverThread = new ServerThread(portField.getText());
                server = new Thread(serverThread);
//                server.setDaemon(true);
                server.start();
            }catch (IOException | ListenException | IllegalArgumentException e) {
                log(e.getMessage());
                return;
            }

            log("Server started!");

            setButtonEnable(stopButton, true);
            setButtonEnable(startButton, false);
            setServerStatus(ServerStatus.ONLINE);
        });
    }

    @FXML
    private void onStopButtonClick() {
        if (serverThread == null) {
            log("Server is not online!");
            return;
        }

        serverThread.closeServer();

        log("Server stopped!");

        setButtonEnable(stopButton, false);
        setButtonEnable(startButton, true);
        setServerStatus(ServerStatus.OFFLINE);
    }















    private void setButtonEnable(Button button, boolean status) {
        if (button == null)
            return;

        button.setDisable(!status);
    }

    public void log(String text) {
        if (text == null || text.isEmpty())
            return;

        textArea.appendText(text + '\n');
    }

    public void setServerStatus(ServerStatus status) {
        statusLabel.setText(STATUS_PREFIX + status);
    }

    private void updateConnectionsLabel(int connections) {
        getConnectionsLabel.setText(CONNECTIONS_PREFIX + connections);
    }

    public void addConnection() {
        connections++;
        updateConnectionsLabel(connections);
    }

    public void removeConnection() {
        connections--;
        updateConnectionsLabel(connections);
    }

}