package pl.jenczalik.validator.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConfigObject {
    private String name;
    private boolean required;
    private ObjectType type;
    private List<String> childrenNames;
}
