package pl.jenczalik.validator.util;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Data
public class CrawlHelper {
    private List<String> parents;
    private List<Map> specificationLayers;
    private List<Map> specificationConfigLayers;

    private boolean isWorking = false;

    public CrawlHelper() {
        parents = new ArrayList<>();
        specificationLayers = new ArrayList<>();
        specificationConfigLayers = new ArrayList<>();
    }

    public void setLayersAndParent(Map specificationLayer, Map specificationConfigLayer, String parent) {
        specificationLayers.add(specificationLayer);
        specificationConfigLayers.add(specificationConfigLayer);
        parents.add(parent);
    }

    public Map getCurrentSpecificationLayer() {
        return specificationLayers.size() > 0 ? specificationLayers.get(specificationLayers.size() - 1) : Collections.emptyMap();
    }

    public Map getCurrectConfigLayer() {
        return specificationConfigLayers.size() > 0 ? specificationConfigLayers.get(specificationConfigLayers.size() - 1) : Collections.emptyMap();
    }

    public String getCurrentParent() {
        return parents.size() > 0 ? parents.get(parents.size() - 1) : "";
    }

    public void removeLastLayersAndParent() {
        int specificationLayersSize = specificationLayers.size();
        int configLayersSize = specificationConfigLayers.size();
        int parentsSize = parents.size();

        specificationLayers.remove(specificationLayersSize - 1);
        specificationConfigLayers.remove(configLayersSize - 1);
        parents.remove(parentsSize - 1);
    }
}
