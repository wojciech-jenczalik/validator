package pl.jenczalik.validator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.jenczalik.validator.model.ValidationResult;
import pl.jenczalik.validator.service.ValidationService;

@RestController
@RequestMapping(value = "/validation")
public class ValidationController {
    private ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping
    public ResponseEntity validate(@RequestBody String yamlSpecification) {
        ValidationResult result = this.validationService.validate(yamlSpecification);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
