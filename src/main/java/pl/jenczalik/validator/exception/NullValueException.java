package pl.jenczalik.validator.exception;

public class NullValueException extends RuntimeException {
    public NullValueException(String key) {
        super(String.format("Null value at key: %s", key));
    }
}