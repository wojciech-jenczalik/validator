package pl.jenczalik.validator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.jenczalik.validator.model.ValidationResult;
import pl.jenczalik.validator.service.ValidationService;

/**
 * Kontroler obsługujący żądania HTTP, których zawartością jest definicja API
 * do zwalidowania.
 */
@RestController
@RequestMapping(value = "/validation")
public class ValidationController {
    private ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Metoda służy do walidacji, czy definicja API spełnia syntaktyczne założenia specyfikacji Coapi.
     *
     * @param yamlApiDefinition Definicja API w formacie YAML, stworzona w języku opisu Coapi.
     * @return Wynik walidacji. Jeśli nie zakończyła się sukcesem, to określa gdzie znajduje się błąd.
     */
    @PostMapping
    public ResponseEntity validate(@RequestBody String yamlApiDefinition) {
        ValidationResult result = this.validationService.validate(yamlApiDefinition);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
