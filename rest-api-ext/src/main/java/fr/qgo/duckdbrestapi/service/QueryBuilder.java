package fr.qgo.duckdbrestapi.service;

import fr.qgo.duckdbrestapi.config.QueryConfig;
import org.sql2o.Connection;
import org.sql2o.Query;

import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public interface QueryBuilder {
    Query prepareQuery(Connection connection, String query, Object params, Map<String, Object> userQueryParams) throws SQLException;
}
