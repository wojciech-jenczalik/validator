package pl.jenczalik.validator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Obiektowa reprezentacja pliku konfiguracyjnego:
 * <p>
 * <a href="file:../../../../../resources/application.yml">/resources/application.yml</a>
 */
@ConfigurationProperties(prefix = "paths")
@Component
public class Config {
    /**
     * Określa nazwę pliku definiującego specyfikację języka.
     */
    @Getter
    @Setter
    private String specification;
}
