package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku, gdy pod danym kluczem nie ma żadnej wartości
 */
public class NullValueException extends RuntimeException {
    public NullValueException(String key) {
        super(String.format("Null value at key: %s", key));
    }
}