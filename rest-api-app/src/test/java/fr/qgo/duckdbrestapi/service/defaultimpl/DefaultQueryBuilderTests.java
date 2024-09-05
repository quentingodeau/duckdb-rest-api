package fr.qgo.duckdbrestapi.service.defaultimpl;

import fr.qgo.duckdbrestapi.service.defaultimpl.query.DefaultQueryBuilder;
import fr.qgo.duckdbrestapi.testtools.DuckDbTestSetup;
import lombok.val;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sql2o.Query;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultQueryBuilderTests extends DuckDbTestSetup {
    private final DefaultQueryBuilder queryBuilder = new DefaultQueryBuilder();

    @BeforeAll
    static void setupTestContext() throws IOException, SQLException {
        setupDbContext();
    }

    @AfterAll
    static void destroy() {
        cleanUpDbContext();
    }


    @Test
    void testQueryFromTable() {
        val queryStr = """
                SELECT str
                FROM MyTable
                WHERE id = :id
                """;
        sql2o.withConnection((connection, argument) -> {
            Query query = queryBuilder.prepareQuery(connection, queryStr, argument, EMPTY_PROPERTIES);
            List<String> values = query.executeScalarList(String.class);
            assertThat(values).containsExactly("a");
        }, Map.of("id", 1));
    }

    @Test
    void testQueryFromParquet() {
        {
            val queryStr = """
                    SELECT str
                    FROM read_parquet('%s/**', hive_partitioning=true)
                    WHERE id = :id
                    """.formatted(parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, EMPTY_PROPERTIES);
                List<String> values = query.executeScalarList(String.class);
                assertThat(values).containsExactly("a");
            }, Map.of("id", 1));
        }

        {
            val queryStr = """
                    SELECT id
                    FROM read_parquet('%s/str=:str/*.parquet', hive_partitioning=true)
                    """.formatted(parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, WITH_PRE_PARSE_PROPERTIES);
                List<Integer> values = query.executeScalarList(Integer.class);
                assertThat(values).containsExactly(1);
            }, Map.of("str", "a"));
        }

        {
            val queryStr = """
                    SELECT id
                    FROM read_parquet('%s/str=:str/*.parquet', hive_partitioning=true)
                    WHERE str = :str
                    """.formatted(parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, WITH_PRE_PARSE_PROPERTIES);
                assertThat(query.getParamNameToIdxMap()).containsKey("str");
                List<Integer> values = query.executeScalarList(Integer.class);
                assertThat(values).containsExactly(1);
            }, Map.of("str", "a"));
        }

        {
            val queryStr = """
                    SELECT id
                    FROM read_parquet(['%s/str=:str/*.parquet'], hive_partitioning=true)
                    """.formatted(parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, WITH_PRE_PARSE_PROPERTIES);
                List<Integer> values = query.executeScalarList(Integer.class);
                assertThat(values).containsExactly(1);
            }, Map.of("str", "a"));
        }

        {
            val queryStr = """
                    SELECT id
                    FROM read_parquet(['%s/str=:str/*.parquet', '%s/str=:str/*.parquet'], hive_partitioning=true)
                    """.formatted(parquet, parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, WITH_PRE_PARSE_PROPERTIES);
                List<Integer> values = query.executeScalarList(Integer.class);
                assertThat(values).containsExactly(1, 1);
            }, Map.of("str", "a"));
        }

        {
            val queryStr = """
                    SELECT id
                    FROM read_parquet(
                        [
                            '%s/str=:str/*.parquet',
                            '%s/str=:str/*.parquet'
                        ],
                         hive_partitioning=true)
                    """.formatted(parquet, parquet);
            sql2o.withConnection((connection, argument) -> {
                Query query = queryBuilder.prepareQuery(connection, queryStr, argument, WITH_PRE_PARSE_PROPERTIES);
                List<Integer> values = query.executeScalarList(Integer.class);
                assertThat(values).containsExactly(1, 1);
            }, Map.of("str", "a"));
        }
    }

    @Test
    void placeholderInPath() {
        String query1 = """
                SELECT id
                FROM read_parquet('%s/str=:str/*.parquet', hive_partitioning=true)
                WHERE str = :str
                """;
        {
            List<DefaultQueryBuilder.PlaceholderPosition> placeholderPositions = DefaultQueryBuilder.placeholderList(query1, 28, 50);
            assertThat(placeholderPositions).hasSize(1);
            DefaultQueryBuilder.PlaceholderPosition placeholderPosition = placeholderPositions.get(0);
            assertThat(query1.substring(placeholderPosition.begin(), placeholderPosition.end())).isEqualTo(":str");
        }

        {
            List<DefaultQueryBuilder.PlaceholderPosition> placeholderPositions = DefaultQueryBuilder.preParse(query1);
            assertThat(placeholderPositions).hasSize(1);
            DefaultQueryBuilder.PlaceholderPosition placeholderPosition = placeholderPositions.get(0);
            assertThat(query1.substring(placeholderPosition.begin(), placeholderPosition.end())).isEqualTo(":str");
        }
    }
}
