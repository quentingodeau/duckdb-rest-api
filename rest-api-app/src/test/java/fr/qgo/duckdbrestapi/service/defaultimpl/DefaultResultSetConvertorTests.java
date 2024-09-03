package fr.qgo.duckdbrestapi.service.defaultimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Query;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultResultSetConvertorTests extends DuckDbTestSetup {
    private final DefaultQueryBuilder queryBuilder = new DefaultQueryBuilder();
    private final DefaultResultSetConvertor resultSetConvertor= new DefaultResultSetConvertor(new ObjectMapper());

    @BeforeAll
    static void setupTestContext() throws IOException, SQLException {
        setupDbContext();
    }

    @AfterAll
    static void destroy() {
        cleanUpDbContext();
    }

    @Test
    void basicTest() {
        val queryStr = """
                SELECT *
                FROM MyTable
                WHERE id = :id
                """;
        sql2o.withConnection((connection, argument) -> {
            Query query = queryBuilder.prepareQuery(connection, queryStr, argument, EMPTY_PROPERTIES);
            val converted = StreamSupport.stream(resultSetConvertor.executeAndFetchLazy(query, EMPTY_PROPERTIES).spliterator(), false).toList();
            assertThat(converted).hasSize(1)
                    .containsExactly("""
                            {"id":1,"str":"a","s":{"i":10,"j":"vak"},"m":{"20":"key2","10":"key1","30":"key3"},"l":["a","b"]}
                            """.trim());
        }, Map.of("id", 1));
    }
}
