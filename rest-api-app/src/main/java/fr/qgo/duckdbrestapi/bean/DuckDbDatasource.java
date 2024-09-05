package fr.qgo.duckdbrestapi.bean;

import com.mchange.v2.c3p0.DataSources;
import fr.qgo.duckdbrestapi.config.AppConfig;
import fr.qgo.duckdbrestapi.config.DuckDbConfig;
import fr.qgo.duckdbrestapi.duckdb.C3P0InitConnection;
import lombok.AllArgsConstructor;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sql2o.Sql2o;
import org.sql2o.quirks.Quirks;
import org.sql2o.quirks.QuirksDetector;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class DuckDbDatasource {
    private final AppConfig appConfig;

    @Bean
    public DataSource duckDbDataSource() throws SQLException {
        DuckDbConfig duckDbConfig = appConfig.getDuckDbConfig();
        DataSource dataSource = DataSources.unpooledDataSource(duckDbConfig.getJdbcUrl(), duckDbConfig.getProperties());
        return DataSources.pooledDataSource(dataSource, getOverrideProps(duckDbConfig));
    }

    private static Map<String, Object> getOverrideProps(DuckDbConfig duckDbConfig) {
        Map<String, Object> overrideProps = new HashMap<>();
        overrideProps.put("connectionCustomizerClassName", C3P0InitConnection.class.getName());
        val initConnectionSql = duckDbConfig.getInitConnectionSql();
        if (initConnectionSql != null) {
            overrideProps.put("extensions", Map.of("initSql", initConnectionSql));
        }
        return overrideProps;
    }

    @Bean
    public Sql2o sql2o(@Qualifier("duckDbDataSource") DataSource dataSource) {
        val duckDbConfig = appConfig.getDuckDbConfig();
        val jdbcUrl = duckDbConfig.getJdbcUrl();
        val quirks = QuirksDetector.forURL(jdbcUrl);
        return new Sql2o(dataSource, quirks);
    }
}
