package fr.qgo.duckdbrestapi.exception;

public class QueryNotFound extends Exception{
    public QueryNotFound(String message) {
        super(message);
    }
}
