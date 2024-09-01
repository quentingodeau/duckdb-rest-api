package fr.qgo.duckdbrestapi.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Map<String, QueryConfig> queries;
    private Properties duckDbConfig = new Properties();
}
