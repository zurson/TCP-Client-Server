module org.example.server {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.server to javafx.fxml;
    exports org.example.server;
    exports org.example.server.Threads;
    opens org.example.server.Threads to javafx.fxml;
}