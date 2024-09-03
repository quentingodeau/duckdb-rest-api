package fr.qgo.duckdbrestapi.controller;

public class PojoCreationError extends Exception {
    public PojoCreationError(String message) {
        super(message);
    }

    public PojoCreationError(String message, Throwable cause) {
        super(message, cause);
    }

    public static  PojoCreationError failedToCreateField(String className, String fieldName, String type) {
        return new PojoCreationError(String.format("Failed to create the field '%s' of type '%s' in class '%s'", fieldName, type, className));
    }
}
