package pl.jenczalik.validator.util.error;

import org.springframework.stereotype.Component;
import pl.jenczalik.validator.model.ValidationCode;
import pl.jenczalik.validator.model.ValidationResult;

/**
 * Klasa służąca do obsługi błędów wykrywanych w trakcie walidacji.
 */
@Component
public class ValidationErrorHandler {
    public ValidationResult handleException(Exception e, String parent) {
        String parentInfo = String.format("Validation error at object: %s. ", parent);
        return new ValidationResult(ValidationCode.EXCEPTION, parentInfo + e.getMessage());
    }
}
