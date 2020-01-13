package pl.jenczalik.validator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "paths")
@Component
public class Config {
    @Getter
    @Setter
    private String specification;
}
