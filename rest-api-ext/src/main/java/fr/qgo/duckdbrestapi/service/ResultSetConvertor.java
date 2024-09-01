package fr.qgo.duckdbrestapi.service;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetConvertor<R> {
    R convert(ResultSet resultSet) throws SQLException;
}
