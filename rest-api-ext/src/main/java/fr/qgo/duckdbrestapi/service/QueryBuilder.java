package fr.qgo.duckdbrestapi.service;

import fr.qgo.duckdbrestapi.config.QueryConfig;
import org.sql2o.Connection;
import org.sql2o.Query;

import java.sql.SQLException;

public interface QueryBuilder<T> {
    Query prepareQuery(Connection connection, QueryConfig query, Object params) throws SQLException;
}
