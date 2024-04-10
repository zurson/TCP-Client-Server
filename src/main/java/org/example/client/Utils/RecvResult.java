package org.example.client.Utils;

public class RecvResult {

    private int bytes;
    private String message;

    public RecvResult(int bytes, String message) {
        this.bytes = bytes;
        this.message = message;
    }

    public int getBytes() {
        return bytes;
    }

    public String getMessage() {
        return message;
    }

}
