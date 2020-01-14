package pl.jenczalik.validator.exception;

public class RequiredObjectNotPresentException extends RuntimeException {
    public RequiredObjectNotPresentException(String objectName) {
        super(String.format("Required object %s is not present.", objectName));
    }
}
