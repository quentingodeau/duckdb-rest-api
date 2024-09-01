package fr.qgo.duckdbrestapi.controller;

import fr.qgo.duckdbrestapi.config.PojoFieldType;

public class PojoCreationError extends Exception {
    public PojoCreationError(String message) {
        super(message);
    }

    public PojoCreationError(String message, Throwable cause) {
        super(message, cause);
    }

    public static  PojoCreationError failedToCreateField(String className, String fieldName, PojoFieldType type) {
        return new PojoCreationError(String.format("Failed to create the field '%s' of type '%s' in class '%s'", fieldName, type, className));
    }
}
