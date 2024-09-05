package fr.qgo.duckdbrestapi.testtools;

import org.sql2o.GenericDatasource;
import org.sql2o.Sql2o;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

public abstract class DuckDbTestSetup {
    protected static final Map<String, Object> EMPTY_PROPERTIES = Map.of();
    protected static final Map<String, Object> WITH_PRE_PARSE_PROPERTIES = Map.of("queryBuilder.preParse", true);

    protected static File parquet;
    protected static File dbFile;
    protected static Sql2o sql2o;

    protected static void setupDbContext() throws IOException, SQLException {
        parquet = Files.createTempDirectory("parquet").toFile();
        dbFile = new File(Files.createTempDirectory("duckdb").toFile(), "test.db");
        try (
                Connection connection = DriverManager.getConnection("jdbc:duckdb:" + dbFile);
                Statement statement = connection.createStatement()
        ) {
            statement.execute("""
                    CREATE TABLE MyTable(
                    id   INTEGER,
                    str  VARCHAR,
                    s    STRUCT(i INTEGER, j VARCHAR),
                    m    MAP(INTEGER, VARCHAR),
                    l    VARCHAR[]
                    )""");
            statement.execute("""
                    INSERT INTO MyTable(id, str, s, m, l) VALUES
                    (1, 'a', {i: 10, j: 'vak'}, MAP {10: 'key1', 20: 'key2', 30: 'key3'}, ['a', 'b']),
                    (2, 'b', {i: 10, j: 'vak'}, MAP {10: 'key1', 20: 'key2', 30: 'key3'}, ['a', 'b']),
                    (3, 'b', {i: 11, j: 'val'}, MAP {11: '1key', 21: '2key', 31: '3key'}, ['c', 'z']),
                    """);

            statement.execute("COPY MyTable TO '%s' (FORMAT PARQUET, PARTITION_BY 'str')".formatted(parquet));
        }


        Properties properties = new Properties();
        properties.setProperty("access_mode", "READ_ONLY");
        sql2o = new Sql2o(new GenericDatasource("jdbc:duckdb:" + dbFile, properties));
    }

    protected static void cleanUpDbContext() {
        sql2o = null;
        parquet.delete();
        dbFile.delete();
    }
}
