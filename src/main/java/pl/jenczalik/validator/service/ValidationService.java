package pl.jenczalik.validator.service;

import com.esotericsoftware.yamlbeans.YamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jenczalik.validator.exception.BadTypeException;
import pl.jenczalik.validator.exception.ExcessiveObjectPresentException;
import pl.jenczalik.validator.exception.NoMatchWithRegexException;
import pl.jenczalik.validator.exception.NullValueException;
import pl.jenczalik.validator.exception.RequiredObjectNotPresentException;
import pl.jenczalik.validator.util.error.ValidationErrorHandler;
import pl.jenczalik.validator.util.parser.YamlParser;
import pl.jenczalik.validator.config.Config;
import pl.jenczalik.validator.model.ValidationResult;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private static final String ROOT = "root";

    private static final String TYPE = "type";
    private static final String REQUIRED = "required";

    private static final String TYPE_STRING = "string";
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_BOOLEAN = "boolean";

    private static final String CHILDREN = "children";
    private static final String VALUE_REGEX = "valueRegex";
    private static final String NAME_REGEX = "nameRegex";

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private final YamlParser yamlParser;
    private final ValidationErrorHandler errorHandler;
    private final Map specificationConfig;
    private final List<String> parents;

    @Autowired
    public ValidationService(YamlParser yamlParser,
                             Config config,
                             ValidationErrorHandler errorHandler) throws YamlException, FileNotFoundException {
        this.yamlParser = yamlParser;
        this.errorHandler = errorHandler;

        String specificationFileName = config.getSpecification();
        this.specificationConfig = this.yamlParser.parseYamlFile(specificationFileName);
        this.parents = new ArrayList<>();
    }

    public ValidationResult validate(String yamlSpecification) {

        try {
            Map<String, ?> specification = this.yamlParser.parseYamlString(yamlSpecification);

            logger.info("Validation started");
            validateObject(specification, specificationConfig);
            logger.info("Validation ended");

            return ValidationResult.ok();
        } catch (Exception e) {
            String parents = this.parents.isEmpty() ? ROOT : String.join(" -> ", this.parents);
            return this.errorHandler.handleException(e, parents);
        } finally {
            parents.clear();
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

            validateForNull(specification, currentKey);

            try {
                validateType(specification.get(currentKey), type);
            } catch (BadTypeException e) {
                throw new BadTypeException(currentKey, type, e.getBadType());
            }

            switch (type) {
                case TYPE_OBJECT:
                    parents.add(currentKey);
                    validateObject((Map) specification.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
                    break;

                case TYPE_ARRAY:
                    parents.add(currentKey);
                    validateArray((Map) specification.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
                    break;

                default:
                    validatePrimitive((String) specification.get(currentKey), (Map) config.get(currentKey), type);
                    break;
            }
            parents.remove(currentKey);
        }
    }

    private void validateArray(Map<String, ?> array, Map config) {
        for (String currentKey : array.keySet()) {
            logger.info(String.format("Array: %s", currentKey));

            //TODO: move it one level above?
            Map arrayObjectConfigLayer = (Map) config.get(config.keySet().iterator().next());

            validateForNull(array, currentKey);
            
            if(arrayObjectConfigLayer.get(NAME_REGEX) != null) {
                validateByRegex(currentKey, (String) arrayObjectConfigLayer.get(NAME_REGEX));
            }

            String type = (String) arrayObjectConfigLayer.get(TYPE);

            try {
                validateType(array.get(currentKey), type);
            } catch (BadTypeException e) {
                if(String.class.equals(e.getBadType()))
                throw new BadTypeException(currentKey, type, e.getBadType());
            }

            switch (type) {
                case TYPE_OBJECT:
                    parents.add(currentKey);
                    validateObject((Map) array.get(currentKey), (Map) arrayObjectConfigLayer.get(CHILDREN));
                    break;

                case TYPE_ARRAY:
                    parents.add(currentKey);
                    validateArray((Map) array.get(currentKey), (Map) arrayObjectConfigLayer.get(CHILDREN));
                    break;

                default:
                    validatePrimitive((String) array.get(currentKey), config, type);
                    break;
            }
            parents.remove(currentKey);
        }
    }

    private void validatePrimitive(String value, Map config, String type) {
        if (TYPE_STRING.equals(type)) {
            validateString(value, config);
        }
    }

    private void validateString(String string, Map config) {
        if(config.get(VALUE_REGEX) != null) {
            validateByRegex(string, (String) config.get(VALUE_REGEX));
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

    private void validateForNull(Map specification, String key) {
        if(specification.get(key) == null) {
            throw new NullValueException(key);
        }
    }

    private void validateType(Object object, String type) {
        switch (type) {
            case TYPE_STRING:
                validateType(object, String.class);
                break;

            case TYPE_BOOLEAN:
                validateType(object, Boolean.class);
                break;

            case TYPE_OBJECT:
            case TYPE_ARRAY:
                validateType(object, Map.class);
                break;
        }
    }

    private void validateType(Object object, Class clazz) {
        if(Boolean.class.equals(clazz)) {
            String string = object.toString();
            if(!TRUE.equalsIgnoreCase(string) && !FALSE.equalsIgnoreCase(string)) {
                throw new BadTypeException(object.getClass());
            }
        } else if(!clazz.isInstance(object)) {
            throw new BadTypeException(object.getClass());
        }
    }

    private Set<String> fetchRequiredFields(Map<String, ?> config) {
        return config.entrySet().stream()
                .filter(entry -> TRUE.equals(((Map) entry.getValue()).get(REQUIRED)))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private Set<String> fetchAllowedFields(Map<String, ?> config) {
        return new HashSet<>(config.keySet());
    }
}