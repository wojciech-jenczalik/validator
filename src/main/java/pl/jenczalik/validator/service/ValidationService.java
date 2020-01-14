package pl.jenczalik.validator.service;

import com.esotericsoftware.yamlbeans.YamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.jenczalik.validator.crawler.SpecificationConfigCrawler;
import pl.jenczalik.validator.crawler.SpecificationCrawler;
import pl.jenczalik.validator.exception.ExcessiveObjectPresentException;
import pl.jenczalik.validator.exception.RequiredObjectNotPresentException;
import pl.jenczalik.validator.model.ConfigObject;
import pl.jenczalik.validator.model.specificationObject.SpecificationObject;
import pl.jenczalik.validator.util.CrawlHelper;
import pl.jenczalik.validator.util.error.ValidationErrorHandler;
import pl.jenczalik.validator.util.parser.YamlParser;
import pl.jenczalik.validator.config.Config;
import pl.jenczalik.validator.model.ValidationResult;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private final String ROOT = "ROOT";

    private final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private final YamlParser yamlParser;
    private final SpecificationCrawler specificationCrawler;
    private final SpecificationConfigCrawler specificationConfigCrawler;
    private final CrawlHelper crawlHelper;
    private final ValidationErrorHandler errorHandler;
    private final Map specificationConfig;

    @Autowired
    public ValidationService(YamlParser yamlParser,
                             SpecificationCrawler specificationCrawler,
                             SpecificationConfigCrawler specificationConfigCrawler,
                             Config config,
                             CrawlHelper crawlHelper,
                             ValidationErrorHandler errorHandler) throws YamlException, FileNotFoundException {
        this.yamlParser = yamlParser;
        this.specificationCrawler = specificationCrawler;
        this.specificationConfigCrawler = specificationConfigCrawler;
        this.crawlHelper = crawlHelper;
        this.errorHandler = errorHandler;

        String specificationFileName = config.getSpecification();
        this.specificationConfig = this.yamlParser.parseYamlFile(specificationFileName);
    }

    public ValidationResult validate(String yamlSpecification) {

        ValidationResult validationResult;

        try {
            Map<String, ?> specification = this.yamlParser.parseYamlString(yamlSpecification);

            crawlHelper.setWorking(true);
            crawlHelper.setLayersAndParent(specification, specificationConfig, ROOT);
            Set<String> keyStorage = new LinkedHashSet<>();
            Set<String> usedKeyStorage = new LinkedHashSet<>();
            Iterator arrayObjectsIterator = null;

            while(crawlHelper.isWorking()) {
                Map<String, Map<String, ?>> specificationLayer = crawlHelper.getCurrentSpecificationLayer();
                Map<String, Map<String, ?>> configLayer = crawlHelper.getCurrectConfigLayer();

                List<SpecificationObject> specificationObjects = specificationCrawler.mapLayerToSpecificationObjects(specificationLayer);
                List<ConfigObject> configObjects = specificationConfigCrawler.mapLayerToConfigObjects(configLayer);

                validateForMandatoryPresence(configObjects, specificationObjects);
                validateForNoExcessivePresence(configObjects, specificationObjects);

                keyStorage.addAll(configLayer.keySet());
                keyStorage.removeAll(usedKeyStorage);

                Iterator<String> keyIterator = keyStorage.iterator();
                String currentParent = crawlHelper.getCurrentParent();
                Map currentSpecificationLayer = crawlHelper.getCurrentSpecificationLayer();
                Map currentConfigLayer = crawlHelper.getCurrectConfigLayer();
                String nextParent = "";
                String keyPlaceholder = "";

                boolean shouldGetNext = false;
                boolean noMoreParentsInLayer = false;

                while(keyIterator.hasNext()) {
                    String key = keyIterator.next();
                    if(key.startsWith("^") || (!shouldGetNext && (currentParent.equals(ROOT) || key.equals(currentParent)))) {
                        shouldGetNext = true;
                    }
                    if(shouldGetNext) {
                        if(key.startsWith("^") || (currentSpecificationLayer.get(key) instanceof Map &&
                            currentConfigLayer.get(key) instanceof Map)) {
                            if(!nextParent.equals(key)) {
                                arrayObjectsIterator = currentSpecificationLayer.keySet().iterator();
                            }
                            nextParent = key;
                            break;
                        }
                    }
                    if(!keyIterator.hasNext()) {
                        noMoreParentsInLayer = true;
                    }
                }

                if(noMoreParentsInLayer) {
                    if(crawlHelper.getSpecificationLayers().size() == 1 ||
                        crawlHelper.getSpecificationConfigLayers().size() == 1) {
                        crawlHelper.setWorking(false);
                    } else {
                        crawlHelper.removeLastLayersAndParent();
                        usedKeyStorage.add(currentParent);
                    }
                } else {
                    if(nextParent.startsWith("^")) {
                        Map<String, ?> nextSpecificationLayer = specificationLayer.get(arrayObjectsIterator.next());
                        Map<String, ?> nextConfigLayer = (Map<String, ?>) configLayer.get(nextParent).get("children");
                        crawlHelper.setLayersAndParent(nextSpecificationLayer, nextConfigLayer, nextParent);
                    }else {
                        Map<String, ?> nextSpecificationLayer = specificationLayer.get(nextParent);
                        Map<String, ?> nextConfigLayer = (Map<String, ?>) configLayer.get(nextParent).get("children");
                        crawlHelper.setLayersAndParent(nextSpecificationLayer, nextConfigLayer, nextParent);
                    }
                }
            }
            validationResult = ValidationResult.ok();

        } catch (Exception e) {
            validationResult = this.errorHandler.handleException(e, crawlHelper.getCurrentParent());
        }

        crawlHelper.reset();

        return validationResult;
    }

    private void validateForMandatoryPresence(List<ConfigObject> configObjects, List<SpecificationObject> specificationObjects) {
        for(ConfigObject configObject : configObjects) {
            if (configObject.isRequired()) {
                if(configObject.getName().startsWith("^")){
                    //...
                } else if (specificationObjects.stream().noneMatch(specObj -> specObj.getKey().equals(configObject.getName()))) {
                    throw new RequiredObjectNotPresentException(configObject.getName());
                }
            }
        }
    }

    private void validateForNoExcessivePresence(List<ConfigObject> configObjects, List<SpecificationObject> specificationObjects) {
        if (configObjects.stream().map(ConfigObject::getName).collect(Collectors.toList()).stream().anyMatch(name -> name.startsWith("^"))) {
            //..
        } else {
            for (SpecificationObject specificationObject : specificationObjects) {
                if (configObjects.stream().map(ConfigObject::getName).noneMatch(cfgObjName -> cfgObjName.equals(specificationObject.getKey()))) {
                    throw new ExcessiveObjectPresentException(specificationObject.getKey());
                }
            }
        }
    }

    private void validateType(List<ConfigObject> configObjects, List<SpecificationObject> specificationObjects) {

    }
}
