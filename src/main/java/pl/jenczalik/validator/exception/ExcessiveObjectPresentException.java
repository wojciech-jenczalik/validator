package pl.jenczalik.validator.exception;

/**
 * Wyjątek rzucany w przypadku obecności nadmiarowego obiektu - nieopisanego w specyfikacji.
 */
public class ExcessiveObjectPresentException extends RuntimeException {
    public ExcessiveObjectPresentException(String objectName) {
        super(String.format("Excessive object %s is present.", objectName));
    }
}
