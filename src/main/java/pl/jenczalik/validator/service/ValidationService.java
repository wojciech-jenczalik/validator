package pl.jenczalik.validator.service;

import com.esotericsoftware.yamlbeans.YamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jenczalik.validator.exception.ExcessiveObjectPresentException;
import pl.jenczalik.validator.exception.NoMatchWithRegexException;
import pl.jenczalik.validator.exception.RequiredObjectNotPresentException;
import pl.jenczalik.validator.util.error.ValidationErrorHandler;
import pl.jenczalik.validator.util.parser.YamlParser;
import pl.jenczalik.validator.config.Config;
import pl.jenczalik.validator.model.ValidationResult;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private static final String TYPE = "type";
    private static final String REQUIRED = "required";

    private static final String TYPE_STRING = "string";
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";

    private static final String CHILDREN = "children";
    private static final String VALUE_REGEX = "valueRegex";
    private static final String NAME_REGEX = "nameRegex";

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private final YamlParser yamlParser;
    private final ValidationErrorHandler errorHandler;
    private final Map specificationConfig;

    @Autowired
    public ValidationService(YamlParser yamlParser,
                             Config config,
                             ValidationErrorHandler errorHandler) throws YamlException, FileNotFoundException {
        this.yamlParser = yamlParser;
        this.errorHandler = errorHandler;

        String specificationFileName = config.getSpecification();
        this.specificationConfig = this.yamlParser.parseYamlFile(specificationFileName);
    }

    public ValidationResult validate(String yamlSpecification) {

        try {
            Map<String, ?> specification = this.yamlParser.parseYamlString(yamlSpecification);

            logger.info("Validation started");

            validateObject(specification, specificationConfig);

            logger.info("Validation ended");

            return ValidationResult.ok();
        } catch (Exception e) {
            return this.errorHandler.handleException(e, "TODO");
        }
    }

    private void validateObject(Map specification, Map config) {
        Set<String> requiredFields = fetchRequiredFields(config);
        Set<String> allowedFields = fetchAllowedFields(config);
        Set<String> specificationKeys = specification.keySet();

        validateForRequiredFields(specificationKeys, requiredFields);
        validateForExcessiveFields(specificationKeys, allowedFields);

        for (String currentKey : specificationKeys) {
            logger.info(String.format("Object: %s", currentKey));

            String type = (String) ((Map) config.get(currentKey)).get(TYPE);

            if (TYPE_STRING.equals(type)) {
                validateString((String) specification.get(currentKey), (Map) config.get(currentKey));
            } else if (TYPE_OBJECT.equals(type)) {
                validateObject((Map) specification.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
            } else if (TYPE_ARRAY.equals(type)) {
                validateArray((Map) specification.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
            }
        }
    }

    private void validateString(String string, Map config) {
        if(config.get(VALUE_REGEX) != null) {
            validateByRegex(string, (String) config.get(VALUE_REGEX));
        }
    }

    private void validateArray(Map<String, ?> array, Map config) {
        for (String currentKey : array.keySet()) {
            logger.info(String.format("Array: %s", currentKey));

            //TODO: move it one level above?
            Map arrayObjectConfigLayer = (Map) config.get(config.keySet().iterator().next());

            if(arrayObjectConfigLayer.get(NAME_REGEX) != null) {
                validateByRegex(currentKey, (String) arrayObjectConfigLayer.get(NAME_REGEX));
            }

            String type = (String) arrayObjectConfigLayer.get(TYPE);

            if (TYPE_STRING.equals(type)) {
                validateString((String) array.get(currentKey), config);
            } else if (TYPE_OBJECT.equals(type)) {
                validateObject((Map) array.get(currentKey), (Map) arrayObjectConfigLayer.get(CHILDREN));
            } else if (TYPE_ARRAY.equals(type)) {
                validateArray((Map) array.get(currentKey), (Map) arrayObjectConfigLayer.get(CHILDREN));
            }
        }
    }

    private void validateForRequiredFields(Set<String> fields, Set<String> requiredFields) {
        for(String requiredField : requiredFields) {
            if (!fields.contains(requiredField)) {
                throw new RequiredObjectNotPresentException(requiredField);
            }
        }
    }

    private void validateForExcessiveFields(Set<String> fields, Set<String> allowedFields) {
        for(String field: fields) {
            if(!allowedFields.contains(field)) {
                throw new ExcessiveObjectPresentException(field);
            }
        }
    }

    private void validateByRegex(String value, String regex) {
        if(!value.matches(regex)) {
            throw new NoMatchWithRegexException(value, regex);
        }
    }

    private Set<String> fetchRequiredFields(Map<String, ?> config) {
        return config.entrySet().stream()
                .filter(entry -> ((Map) entry.getValue()).get(REQUIRED).equals("true"))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Set<String> fetchAllowedFields(Map<String, ?> config) {
        return new HashSet<>(config.keySet());
    }
}