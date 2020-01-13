package pl.jenczalik.validator.crawler;

import org.springframework.stereotype.Component;
import pl.jenczalik.validator.model.ConfigObject;
import pl.jenczalik.validator.model.ObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SpecificationConfigCrawler {

    private final String REQUIRED = "required";
    private final String CHILDREN = "children";
    private final String TYPE = "type";

    public List<ConfigObject> mapLayerToConfigObjects(Map<String, Map<String, ?>> specificationConfigLayer) {
        List<ConfigObject> layer = new ArrayList<>();

        specificationConfigLayer.forEach((key, value) -> {
            boolean isRequired = Boolean.parseBoolean((String) value.get(REQUIRED));
            Map<String, ?> childrenNamesMap = value.containsKey(CHILDREN) ? (Map) value.get(CHILDREN) : Collections.emptyMap();
            ObjectType type = ObjectType.valueOf((String) value.get(TYPE));
            List<String> childrenNames = new ArrayList<>();

            (childrenNamesMap).forEach((childKey, childValue) -> childrenNames.add(childKey));

            ConfigObject configObject = ConfigObject.builder()
                    .name(key)
                    .required(isRequired)
                    .childrenNames(childrenNames)
                    .type(type)
                    .build();

            layer.add(configObject);
        });

        return layer;
    }
}
