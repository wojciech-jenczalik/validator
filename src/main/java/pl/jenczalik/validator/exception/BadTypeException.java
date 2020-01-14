package pl.jenczalik.validator.exception;

import lombok.Getter;

public class BadTypeException extends RuntimeException {
    @Getter
    private String badType;

    public BadTypeException(Class type) {
        super();
        this.badType = type.getTypeName();
    }

    public BadTypeException(String objectKey, String requiredType, String actualType) {
        super(String.format("Object %s is of bad type. Required type is: %s. Actual type is: %s", objectKey, requiredType, actualType));
    }
}