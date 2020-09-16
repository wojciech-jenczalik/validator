package pl.jenczalik.validator.util.parser;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parser dla formatu YAML.
 */
@Component
public class YamlParser {
    private ClassLoader classLoader;

    public YamlParser() {
        classLoader = YamlParser.class.getClassLoader();
    }

    /**
     * Metoda służąca do marshallingu zawartości pliku YAML do postaci obiektowej.
     *
     * @param filename Nazwa pliku.
     * @return Mapa zawierająca strukturę obiektów zawartych w pliku YAML.
     * @throws YamlException Wyjątek rzucany, gdy zawartość dokumentu nie spełnia założeń formatu YAML.
     * @throws FileNotFoundException Wyjąetk rzucany, gdy plik nie został znaleziony.
     */
    public Map parseYamlFile(String filename) throws YamlException, FileNotFoundException {
        var file = new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());
        YamlReader reader = new YamlReader(new FileReader(file));
        return reader.read(LinkedHashMap.class);
    }

    /**
     * Metoda służąca do marshallingu zawartości tekstu w formacie YAML do postaci obiektowej.
     * @param yaml Tekst w formacie YAML.
     * @return Mapa zawierająca strukturę obiektów zawartych w tekście.
     * @throws YamlException Wyjątek rzucany, gdy zawartość dokumentu nie spełnia założeń formatu YAML.
     */
    public Map parseYamlString(String yaml) throws YamlException {
        YamlReader reader = new YamlReader(yaml);
        return reader.read(LinkedHashMap.class);
    }
}
