package pl.jenczalik.validator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpecificationValue<V> {
    private V value;
}
