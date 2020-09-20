package pl.jenczalik.validator.service;

import com.esotericsoftware.yamlbeans.YamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jenczalik.validator.exception.BadNumberFormatException;
import pl.jenczalik.validator.exception.BadTypeException;
import pl.jenczalik.validator.exception.ExcessiveObjectPresentException;
import pl.jenczalik.validator.exception.NoMatchWithRegexException;
import pl.jenczalik.validator.exception.NullValueException;
import pl.jenczalik.validator.exception.NumberTooLargeException;
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

/**
 * Klasa, której przeznaczeniem jest walidacja prymitywów, tablic i obiektów,
 * pod kątem zgodności z syntaktycznym modelem języka.
 */
@Service
public class ValidationService {

    private static final String ROOT = "root";

    private static final String TYPE = "type";
    private static final String REQUIRED = "required";

    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_UNSIGNED_INTEGER = "unsignedInteger";
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

    /**
     * Metoda wywoływana z kontrolera, w celu walidacji poprawności definicji API.
     * <p>
     * W trakcie walidacji śledzone jest, w którym miejscu definicji prowadzona jest
     * aktualnie walidacja, dzięki czemu w przypadku wykrycia błędu, pokazywana jest
     * ścieżka do miejsca w którym wystąpił błąd.
     *
     * @param yamlApiDefinition Definicja API
     * @return Rezultat walidacji
     * @see ValidationService#validateObject(Map, Map) Metoda wywoływana przez tę metodę.
     */
    public ValidationResult validate(String yamlApiDefinition) {

        try {
            Map<String, ?> apiDefinition = this.yamlParser.parseYamlString(yamlApiDefinition);

            logger.info("Validation started");
            validateObject(apiDefinition, specificationConfig);
            logger.info("Validation ended");

            return ValidationResult.ok();
        } catch (Exception e) {
            String parents = this.parents.isEmpty() ? ROOT : String.join(" -> ", this.parents);
            return this.errorHandler.handleException(e, parents);
        } finally {
            parents.clear();
        }
    }

    /**
     * Metoda wywoływana w celu walidacji obiektu w definicji API w oparciu o model języka.
     * <p>
     * Walidacja obiektu polega na sprawdzeniu, czy obecne są wymagane pola, czy nie ma
     * pól nadmiarowych, czy typy reprezentowane przez pola są zgodne z modelem, oraz
     * czy wartością obiektu nie jest null.
     * <p>
     * Następnie, w zależności od typu obiektów-dzieci, jest wywoływana jedna z trzech metod -
     * <p><ul>
     * <li>Walidacja obiektu
     * <li>Walidacja tablicy
     * <li>Walidacja prymitywu
     * </ul><p>
     *
     * @param apiDefinition Definicja API
     * @param config Model języka
     *
     * @throws BadTypeException Wyjątek rzucany w przypadku złego typu obiektu-dziecka
     *
     * @see ValidationService#validateForRequiredFields(Set, Set) Wywołanie metody walidującej obowiązkowe pola
     * @see ValidationService#validateForExcessiveFields(Set, Set) Wywołanie metody walidującej nadmiarowe pola
     * @see ValidationService#validateForNull(Map, String) Wywołanie metody walidującej wartości null
     * @see ValidationService#validateType(Object, String) Wywołanie metody walidującej zgodność typu
     * @see ValidationService#validateObject(Map, Map) Rekurencyjne wywołanie metody walidującej obiekt
     * @see ValidationService#validateArray(Map, Map) Rekurencyjne wywołanie metody walidującej tablicę
     * @see ValidationService#validatePrimitive(String, Map, String) Wywołanie metody walidującej prymityw
     */
    private void validateObject(Map apiDefinition, Map config) {
        Set<String> requiredFields = fetchRequiredFields(config);
        Set<String> allowedFields = fetchAllowedFields(config);
        Set<String> apiDefinitionKeys = apiDefinition.keySet();

        validateForRequiredFields(apiDefinitionKeys, requiredFields);
        validateForExcessiveFields(apiDefinitionKeys, allowedFields);

        for (String currentKey : apiDefinitionKeys) {
            logger.info(String.format("Object: %s", currentKey));

            String type = (String) ((Map) config.get(currentKey)).get(TYPE);

            validateForNull(apiDefinition, currentKey);

            try {
                validateType(apiDefinition.get(currentKey), type);
            } catch (BadTypeException e) {
                throw new BadTypeException(currentKey, type, e.getBadType());
            }

            switch (type) {
                case TYPE_OBJECT:
                    parents.add(currentKey);
                    validateObject((Map) apiDefinition.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
                    break;

                case TYPE_ARRAY:
                    parents.add(currentKey);
                    validateArray((Map) apiDefinition.get(currentKey), (Map) ((Map) config.get(currentKey)).get(CHILDREN));
                    break;

                default:
                    validatePrimitive((String) apiDefinition.get(currentKey), (Map) config.get(currentKey), type);
                    break;
            }
            parents.remove(currentKey);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji tablicy w definicji API w oparciu o model języka.
     * <p>
     * Walidacja tablicy polega na sprawdzeniu, czy wartością tablicy nie jest null,
     * czy nazwy pól tablicy są zgodne z wyrażeniem regularnym które powinny spełniać,
     * czy typy reprezentowane przez pola są zgodne z modelem.
     * <p>
     * Następnie, w zależności od typu obiektów-dzieci, jest wywoływana jedna z trzech metod -
     * <p><ul>
     * <li>Walidacja obiektu
     * <li>Walidacja tablicy
     * <li>Walidacja prymitywu
     * </ul><p>
     *
     * @param array Tablica
     * @param config Model języka
     *
     * @throws BadTypeException Wyjątek rzucany w przypadku złego typu obiektu - elementu tablicy
     *
     * @see ValidationService#validateForNull(Map, String) Wywołanie metody walidującej wartości null
     * @see ValidationService#validateType(Object, String) Wywołanie metody walidującej zgodność typu
     * @see ValidationService#validateByRegex(String, String) Wywołanie metody walidującej zgodność z wyrażeniem regularnym
     * @see ValidationService#validateObject(Map, Map) Rekurencyjne wywołanie metody walidującej obiekt
     * @see ValidationService#validateArray(Map, Map) Rekurencyjne wywołanie metody walidującej tablicę
     * @see ValidationService#validatePrimitive(String, Map, String) Wywołanie metody walidującej prymityw
     */
    private void validateArray(Map<String, ?> array, Map config) {
        for (String currentKey : array.keySet()) {
            logger.info(String.format("Array: %s", currentKey));

            Map arrayObjectConfigLayer = (Map) config.get(config.keySet().iterator().next());

            validateForNull(array, currentKey); // TODO What if array has only not-required fields? Could allow null probably

            if(arrayObjectConfigLayer.get(NAME_REGEX) != null) {
                validateByRegex(currentKey, (String) arrayObjectConfigLayer.get(NAME_REGEX));
            }

            String type = (String) arrayObjectConfigLayer.get(TYPE);

            try {
                validateType(array.get(currentKey), type);
            } catch (BadTypeException e) {
                if(String.class.toString().equals(e.getBadType())) // TODO works?
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

    /**
     * Metoda wywoływana w celu walidacji prymitywu w definicji API w oparciu o model języka.
     * <p>
     * Walidacja prymitywu polega na sprawdzeniu, czy jego wartość zgodna jest
     * z typem który go definiuje.
     *
     * @param value Wartość prymitywu
     * @param config Model języka
     *               
     * @see ValidationService#validateString(String, Map) Wywołanie metody walidującej string
     * @see ValidationService#validateInteger(String) Wywołanie metody walidującej integer
     * @see ValidationService#validateUnsignedInteger(String) Wywołanie metody walidującej unsigned integer
     */
    private void validatePrimitive(String value, Map config, String type) {
        if (TYPE_STRING.equals(type)) {
            validateString(value, config);
        } else if (TYPE_INTEGER.equals(type)) {
            validateInteger(value);
        } else if (TYPE_UNSIGNED_INTEGER.equals(type)) {
            validateUnsignedInteger(value);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji stringa
     * <p>
     * Walidacja stringa polega na sprawdzeniu, czy jego wartość spełnia wymagania
     * wyrażenia regularnego, o ile takie występuje.
     *
     * @param string String
     * @param config Model języka
     *
     * @see ValidationService#validateByRegex(String, String) Wywołanie metody walidującej wyrażenie regularne
     */
    private void validateString(String string, Map config) {
        if(config.get(VALUE_REGEX) != null) {
            validateByRegex(string, (String) config.get(VALUE_REGEX));
        }
    }

    /**
     * Metoda wywoływana w celu walidacji integera.
     * <p>
     * Walidacja integera polega na sprawdzeniu, czy nie jest on zbyt wielki,
     * oraz czy spełnia format liczbowy.
     *
     * @param value Integer
     *
     * @throws NumberTooLargeException Wyjątek rzucany w przypadku, gdy liczba jest zbyt wielka
     * @throws BadNumberFormatException Wyjątek rzucany w przypadku, gdy zadany integer nie ma formatu liczbowego
     */
    private void validateInteger(String value) {
        try {
            long number = Long.parseLong(value);
            if(number > Integer.MAX_VALUE) {
                throw new NumberTooLargeException(value, TYPE_INTEGER);
            }
        } catch (NumberFormatException e) {
            throw new BadNumberFormatException(value, TYPE_INTEGER);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji unsigned integera.
     * <p>
     * Walidacja integera polega na sprawdzeniu, czy nie jest on zbyt wielki,
     * czy spełnia format liczbowy, oraz czy nie jest zbyt wielki.
     *
     * @param value Unsigned integer
     *
     * @throws NumberTooLargeException Wyjątek rzucany w przypadku, gdy liczba jest zbyt wielka
     * @throws BadNumberFormatException Wyjątek rzucany w przypadku, gdy liczba nie spełnia
     * formatu liczbowego, lub gdy jest mniejsza od zera
     */
    private void validateUnsignedInteger(String value) {
        try {
            long number = Long.parseLong(value);
            if(number > Integer.MAX_VALUE) {
                throw new NumberTooLargeException(value, TYPE_UNSIGNED_INTEGER);
            }
        } catch (NumberFormatException e) {
            throw new BadNumberFormatException(value, TYPE_UNSIGNED_INTEGER);
        }
        if(Integer.parseInt(value) < 0) {
            throw new BadNumberFormatException(value, TYPE_UNSIGNED_INTEGER);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji zbioru pól pod kątem ich wymaganej obecności.

     * @param fields Pola
     * @param requiredFields Wymagane pola
     *
     * @throws RequiredObjectNotPresentException Wyjątek rzucany w przypadku, gdy brakuje wymaganego obiektu
     */
    private void validateForRequiredFields(Set<String> fields, Set<String> requiredFields) {
        for(String requiredField : requiredFields) {
            if (!fields.contains(requiredField)) {
                throw new RequiredObjectNotPresentException(requiredField);
            }
        }
    }

    /**
     * Metoda wywoływana w celu walidacji zbioru pod kątem pól pod kątem nadmiarowej ich liczby.
     *
     * @param fields Pola
     * @param allowedFields Dozwolone pola
     *
     * @throws ExcessiveObjectPresentException Wyjątek rzucany w przypadku, gdy obecne jest nadmiarowe pole
     */
    private void validateForExcessiveFields(Set<String> fields, Set<String> allowedFields) {
        for(String field: fields) {
            if(!allowedFields.contains(field)) {
                throw new ExcessiveObjectPresentException(field);
            }
        }
    }

    /**
     * Metoda wywoływana w celu walidacji wartości w oparciu o wyrażenie regularne.
     * 
     * @param value Wartość
     * @param regex Wyrażenie regularne
     *              
     * @throws NoMatchWithRegexException Wyjątek rzucany w przypadku, gdy wartość nie spełnia reguły wyrażenia regularnego
     */
    private void validateByRegex(String value, String regex) {
        if(!value.matches(regex)) {
            throw new NoMatchWithRegexException(value, regex);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji, czy wartość jest nullem
     *
     * @param apiDefinition Wycinek definicji API
     * @param key Klucz do walidowanej wartości
     *
     * @throws NullValueException Wyjątek rzucany w przypadku, gdy pod kluczem kryje się wartość null
     */
    private void validateForNull(Map apiDefinition, String key) {
        if(apiDefinition.get(key) == null) {
            throw new NullValueException(key);
        }
    }

    /**
     * Metoda wywoływana w celu walidacji, czy typ obiektu jest zgodny z deklarowanym.
     *
     * @param object Walidowany obiekt
     * @param type Deklarowany typ
     *
     * @see ValidationService#validateType(Object, Class) Wywołanie metody walidującą zgodność obiektu z konkretnym typem
     */
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

    /**
     * Metoda wywoływana w celu walidacji, czy typ obiektu jest zgodny z konkretnym typem.
     *
     * @param object Walidowany obiekt
     * @param clazz Konkretny typ
     *
     * @throws BadTypeException Wyjątek rzucany w przypadku, gdy typ obiektu nie jest zgodny z konkretnym typem
     */
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

    /**
     * Pomocnicza metoda wyciągająca zbiór wymaganych pól ze specyfikacji modelu języka.
     *
     * @param config Specyfikacja modelu języka
     * @return Zbiór wymaganych pól
     */
    private Set<String> fetchRequiredFields(Map<String, ?> config) {
        return config.entrySet().stream()
                .filter(entry -> TRUE.equals(((Map) entry.getValue()).get(REQUIRED)))
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * Pomocnicza metoda wyciągająca zbiór dozwolonych pól ze specyfikacji modelu języka.
     *
     * @param config Specyfikacja modelu języka
     * @return Zbiór dozwolonych pól
     */
    private Set<String> fetchAllowedFields(Map<String, ?> config) {
        return new HashSet<>(config.keySet());
    }
}