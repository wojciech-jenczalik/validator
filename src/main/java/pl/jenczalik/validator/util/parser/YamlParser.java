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

@Component
public class YamlParser {
    private ClassLoader classLoader;

    public YamlParser() {
        classLoader = YamlParser.class.getClassLoader();
    }

    public Map parseYamlFile(String filename) throws YamlException, FileNotFoundException {
        var file = new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());
        YamlReader reader = new YamlReader(new FileReader(file));
        return reader.read(LinkedHashMap.class);
    }

    public Map parseYamlString(String yaml) throws YamlException {
        YamlReader reader = new YamlReader(yaml);
        return reader.read(LinkedHashMap.class);
    }
}
