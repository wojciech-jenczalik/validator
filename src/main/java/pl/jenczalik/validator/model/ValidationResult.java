package pl.jenczalik.validator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private ValidationCode validationCode;
    private String validationMessage;

    public static ValidationResult ok() {
        ValidationResult result =  new ValidationResult();
        result.setValidationCode(ValidationCode.OK);
        result.setValidationMessage("No errors found.");
        return result;
    }
}
