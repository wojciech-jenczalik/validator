package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku, gdy brakuje obiektu który jest wymagany.
 */
public class RequiredObjectNotPresentException extends RuntimeException {
    public RequiredObjectNotPresentException(String objectName) {
        super(String.format("Required object %s is not present.", objectName));
    }
}
