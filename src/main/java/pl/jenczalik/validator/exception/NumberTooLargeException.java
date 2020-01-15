package pl.jenczalik.validator.exception;

public class NumberTooLargeException extends RuntimeException{
    public NumberTooLargeException(String value, String type) {
        super(String.format("Number %s is too large for it's type: %s", value, type));
    }
}