package org.example.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.client.Enums.ConnectionStatus;
import org.example.client.Exceptions.ManagerInitializeException;
import org.example.client.Exceptions.SendRecvException;
import org.example.client.Utils.RecvResult;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    private static final String ALREADY_CONNECTED_MSG = "Already connected to the server!";
    private static final String NOT_CONNECTED_MSG = "You are not connected to a server!";
    private static final String STATUS_PREFIX = "Status: ";
    private static final String LACK_OF_MESSAGE_TO_SEND_MSG = "First input a message to send!";
    private static final String DISCONNECTED_MESSAGE = "Disconnected from the server!";
    private static final String CONNECTED_MESSAGE = "Connected to the server!";
    private static final String PORT_LESS_THAN_ZERO_MSG = "Port cannot be less than zero!";
    private static final String SERVER_BUSY = "SERVER BUSY";


    @FXML
    private TextArea textArea;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField messageField;

    @FXML
    private TextField ipField;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private TextField portField;

    private ConnectionManager connectionManager;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setConnectionStatus(ConnectionStatus.OFFLINE);
        setButtonEnable(disconnectButton, false);
        setButtonEnable(connectButton, true);
    }

    private String getIp() {
        return ipField.getText();
    }

    private int getPort() throws NumberFormatException {
        int port = Integer.parseInt(portField.getText());

        if (port < 0)
            throw new NumberFormatException(PORT_LESS_THAN_ZERO_MSG);

        return port;
    }

    private void setButtonEnable(Button button, boolean status) {
        if (button == null)
            return;

        button.setDisable(!status);
    }

    private boolean isConnected() {
        if (connectionManager == null)
            return false;

        return connectionManager.isConnected();
    }

    private void establishConnection() {
        String ip = getIp();
        int port;

        try {
            port = getPort();

            connectionManager = new ConnectionManager(ip, port);
            connectionManager.connect();

            log(CONNECTED_MESSAGE);
            setConnectionStatus(ConnectionStatus.ONLINE);
            setButtonEnable(disconnectButton, true);
            setButtonEnable(connectButton, false);

            RecvResult recvResult = connectionManager.recvMessage();
            if (recvResult.getMessage().equals(SERVER_BUSY)) {
                log(SERVER_BUSY);
                connectionManager.disconnect();
                disconnect();
            }

        } catch (NumberFormatException | ManagerInitializeException | SendRecvException | IOException e) {
            log(e.getMessage());
            connectionManager.closeStreams();
            connectionManager = null;
            setButtonEnable(disconnectButton, false);
            setButtonEnable(connectButton, true);
        }
    }

    private String getMessageToSend() {
        String message = messageField.getText();

        return message == null || message.isEmpty() ? null : message;
    }

    private void sendMsg(String message) throws SendRecvException, IOException {
        int bytes = connectionManager.sendMessage(message);

        if (message.length() > ConnectionManager.MAX_BYTES)
            message = message.substring(0, ConnectionManager.MAX_BYTES);

        log(String.format("Sent (%dB): %s", bytes, message));
    }

    private void recvMsg() throws IOException {
        RecvResult recvResult = connectionManager.recvMessage();
        log(String.format("Recv (%dB): %s", recvResult.getBytes(), recvResult.getMessage()));
    }

    private void sendAndRecvMessage(String message) {
        try {
            sendMsg(message);
            recvMsg();
        } catch (IOException | SendRecvException e) {
            log(e.getMessage());
            onDisconnectButtonClick();
        }
    }

    private void disconnect() {
        try {
            connectionManager.disconnect();
            log(DISCONNECTED_MESSAGE);
            setConnectionStatus(ConnectionStatus.OFFLINE);
        } catch (IOException e) {
            log(e.getMessage());
        } finally {
            connectionManager.closeStreams();
            connectionManager = null;
            setButtonEnable(disconnectButton, false);
            setButtonEnable(connectButton, true);
        }
    }


    @FXML
    private void onConnectButtonClick() {
        if (isConnected()) {
            log(ALREADY_CONNECTED_MSG);
            return;
        }

        establishConnection();
    }


    @FXML
    private void onDisconnectButtonClick() {
        if (!isConnected()) {
            log(NOT_CONNECTED_MSG);
            return;
        }

        disconnect();
    }


    @FXML
    private void onSendMessageButtonClick() {
        if (!isConnected()) {
            log(NOT_CONNECTED_MSG);
            return;
        }

        String message = getMessageToSend();
        if (message == null) {
            log(LACK_OF_MESSAGE_TO_SEND_MSG);
            return;
        }

        sendAndRecvMessage(message);
    }


    public void setConnectionStatus(ConnectionStatus status) {
        statusLabel.setText(STATUS_PREFIX + status);
    }

    public void log(String text) {
        if (text == null)
            return;

        textArea.appendText(text + '\n');
    }


}