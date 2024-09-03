package fr.qgo.duckdbrestapi.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Properties;

@Getter
@Setter
@ToString
public class DuckDbConfig {
    private String jdbcUrl = "jdbc:duckdb:";
    private Properties properties = new Properties();
}
