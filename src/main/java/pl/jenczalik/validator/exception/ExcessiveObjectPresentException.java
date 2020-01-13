package pl.jenczalik.validator.exception;

public class ExcessiveObjectPresentException extends RuntimeException {
    public ExcessiveObjectPresentException(String objectName) {
        super("Excessive object \"" + objectName + "\" is present.");
    }
}
