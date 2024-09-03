package fr.qgo.duckdbrestapi.service;

import org.sql2o.Query;
import org.sql2o.ResultSetIterable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public interface ResultSetConvertor<R> {
    ResultSetIterable<R> executeAndFetchLazy(Query query, Properties userQueryParams) throws SQLException;
}
