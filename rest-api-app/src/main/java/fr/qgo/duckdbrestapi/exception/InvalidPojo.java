package fr.qgo.duckdbrestapi.exception;

public class InvalidPojo extends RuntimeException{
    public InvalidPojo(String message) {
        super(message);
    }

    public InvalidPojo(String message, Throwable cause) {
        super(message, cause);
    }
}
