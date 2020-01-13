package pl.jenczalik.validator.model.specificationObject;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpecificationObject<V> {
    private String key;
    private V value;
}
