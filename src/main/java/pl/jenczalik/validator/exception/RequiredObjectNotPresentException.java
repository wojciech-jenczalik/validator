package pl.jenczalik.validator.exception;

public class RequiredObjectNotPresentException extends RuntimeException {
    public RequiredObjectNotPresentException(String objectName) {
        super("Required object \"" + objectName + "\" is not present.");
    }
}
