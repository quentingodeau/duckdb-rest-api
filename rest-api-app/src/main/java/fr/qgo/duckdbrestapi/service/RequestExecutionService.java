package fr.qgo.duckdbrestapi.service;

import fr.qgo.duckdbrestapi.config.AppConfig;
import fr.qgo.duckdbrestapi.config.QueryConfig;
import fr.qgo.duckdbrestapi.exception.QueryNotFound;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;

@Service
public class RequestExecutionService {
    private final Sql2o duckDbDataSource;
    private final Map<String, QueryConfig> queries;
    private final ApplicationContext applicationContext;

    public RequestExecutionService(
            Sql2o duckDbDataSource,
            AppConfig appConfig,
            ApplicationContext applicationContext
    ) {
        this.duckDbDataSource = duckDbDataSource;
        this.queries = appConfig.getQueries();
        this.applicationContext = applicationContext;
    }

    public ResponseEntity<StreamingResponseBody> runQuery(String queryId, Object params) throws QueryNotFound, SQLException {
        QueryConfig queryConfig = queries.get(queryId);
        if (queryConfig == null) {
            throw new QueryNotFound("No query found with id '" + queryId + "'");
        }
        return ResponseEntity.ok(runQuery(queryConfig, params));
    }

    private StreamingResponseBody runQuery(QueryConfig query, Object params) throws SQLException {
        val queryBuilder = applicationContext.getBean(query.getQueryBuilderClass());
        val resultSetConvertor = applicationContext.getBean(query.getResultSetConvertorClass());
        val jsonConverter = applicationContext.getBean(query.getJsonConvertorClass());

//        val connection = duckDbDataSource.getConnection();
//        val statement = queryBuilder.prepareQuery(connection, query, params);
//        val resultSet = statement.executeQuery();
//        val duckDBResultSet = resultSet.unwrap(DuckDBResultSet.class);
//        val stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
//                        new Iterator<DuckDBResultSet>() {
//                            @Override
//                            @SneakyThrows
//                            public boolean hasNext() {
//                                return duckDBResultSet.next();
//                            }
//
//                            @Override
//                            public DuckDBResultSet next() {
//                                return duckDBResultSet;
//                            }
//                        }, Spliterator.IMMUTABLE
//                ), false)
//                .map(sneaky(resultSetConvertor::convert))
//                .map(elt -> unsafeToJsonStr(elt, jsonConverter))
//                .onClose(sneaky(resultSet::close))
//                .onClose(sneaky(statement::close))
//                .onClose(sneaky(connection::close));

        return (outputStream) -> {
            duckDbDataSource.withConnection((connexion, args) -> {
                Query prepareQuery = queryBuilder.prepareQuery(connexion, query, args);
                ResultSetIterable<?> objects = prepareQuery.executeAndFetchLazy(resultSetConvertor::convert);
                objects.forEach(elt -> {
                    String outLine = unsafeToJsonStr(elt, jsonConverter);
                    byte[] bytes = outLine.getBytes(StandardCharsets.UTF_8);
                    try {
                        outputStream.write(bytes);
                        outputStream.write('\n');
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }, params);
//            try (stream) {
//                Iterator<String> iterator = stream.iterator();
//                while (iterator.hasNext()) {
//                    String data = iterator.next();
//                    byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
//                    outputStream.write(bytes);
//                }
//            }
        };
    }

    private <T> String unsafeToJsonStr(Object data, JsonConvertor<T> converter) {
        return converter.toJsonStr((T) data);
    }

    private interface SneakyRunnableException {
        void run() throws SQLException;
    }


    private interface SneakyFunctionException<T, R> {
        R apply(T elt) throws SQLException;
    }
    private static <T, R> Function<T, R> sneaky(SneakyFunctionException<T, R> sneakyActionException) {
        return new Function<T, R>() {
            @Override
            @SneakyThrows(SQLException.class)
            public R apply(T elt) {
                return sneakyActionException.apply(elt);
            }
        };
    }

    private static Runnable sneaky(SneakyRunnableException sneakyActionException) {
        return new Runnable() {
            @Override
            @SneakyThrows(SQLException.class)
            public void run()  {
                sneakyActionException.run();
            }
        };
    }
}
