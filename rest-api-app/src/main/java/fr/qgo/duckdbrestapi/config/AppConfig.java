package fr.qgo.duckdbrestapi.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Map<String, QueryConfig> queries = new HashMap<>();
    private DuckDbConfig duckDbConfig = new DuckDbConfig();
}
