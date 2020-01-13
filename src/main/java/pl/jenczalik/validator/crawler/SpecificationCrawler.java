package pl.jenczalik.validator.crawler;

import org.springframework.stereotype.Component;
import pl.jenczalik.validator.model.specificationObject.SpecificationObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SpecificationCrawler {

    public List<SpecificationObject> mapLayerToSpecificationObjects(Map<String, ?> specification) {
        Set<SpecificationObject> layer = new HashSet<>();
        specification.forEach((key, value) -> {
            SpecificationObject specificationObject = new SpecificationObject<>(key, value);
            layer.add(specificationObject);
        });

        return new ArrayList<>(layer);
    }
}
