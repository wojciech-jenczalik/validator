package pl.jenczalik.validator.exception;

public class NoMatchWithRegexException extends RuntimeException {
    public NoMatchWithRegexException(String objectName, String regex) {
        super(String.format("Object %s does not match %s regex pattern.", objectName, regex));
    }
}
