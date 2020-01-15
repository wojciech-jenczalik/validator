package pl.jenczalik.validator.exception;

public class BadNumberFormatException extends RuntimeException {
    public BadNumberFormatException(String value, String type) {
        super(String.format("Value %s has incorrect type. Required type is: %s.", value, type));
    }
}