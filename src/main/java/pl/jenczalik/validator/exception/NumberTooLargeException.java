package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku, gdy liczba jest zbyt wielka względem swojego typu.
 */
public class NumberTooLargeException extends RuntimeException{
    public NumberTooLargeException(String value, String type) {
        super(String.format("Number %s is too large for it's type: %s", value, type));
    }
}