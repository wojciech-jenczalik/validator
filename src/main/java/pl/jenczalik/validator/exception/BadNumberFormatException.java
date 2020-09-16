package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku złego formatu liczby.
 */
public class BadNumberFormatException extends RuntimeException {
    public BadNumberFormatException(String value, String type) {
        super(String.format("Value %s has incorrect type. Required type is: %s.", value, type));
    }
}