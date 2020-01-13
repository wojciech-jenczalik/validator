package pl.jenczalik.validator.util.error;

import org.springframework.stereotype.Component;
import pl.jenczalik.validator.model.ValidationCode;
import pl.jenczalik.validator.model.ValidationResult;

@Component
public class ValidationErrorHandler {
    public ValidationResult handleException(Exception e) {
        return new ValidationResult(ValidationCode.EXCEPTION, e.getMessage());
    }
}
