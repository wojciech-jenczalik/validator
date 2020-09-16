package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku, gdy nazwa nie spełnia wymogów wyrażenia regularnego.
 */
public class NoMatchWithRegexException extends RuntimeException {
    public NoMatchWithRegexException(String objectName, String regex) {
        super(String.format("Object %s does not match %s regex pattern.", objectName, regex));
    }
}
