package fr.qgo.duckdbrestapi.service;

import fr.qgo.duckdbrestapi.config.AppConfig;
import fr.qgo.duckdbrestapi.config.QueryConfig;
import fr.qgo.duckdbrestapi.exception.QueryNotFound;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;
import org.sql2o.StatementRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestExecutionService {
    private final Sql2o duckDbDataSource;
    private final Map<String, QueryConfig> queries;
    private final ApplicationContext applicationContext;
    private final Map<String, QueryContext> queryContextCacheMap = new ConcurrentHashMap<>();

    public RequestExecutionService(
            Sql2o duckDbDataSource,
            AppConfig appConfig,
            ApplicationContext applicationContext
    ) {
        this.duckDbDataSource = duckDbDataSource;
        this.queries = appConfig.getQueries();
        this.applicationContext = applicationContext;
    }

    public ResponseEntity<StreamingResponseBody> runQuery(String queryId, Object params) throws QueryNotFound {
        QueryConfig queryConfig = queries.get(queryId);
        if (queryConfig == null) {
            throw new QueryNotFound("No query found with id '" + queryId + "'");
        }
        return ResponseEntity.ok(runQuery(queryId, queryConfig, params));
    }

    private StreamingResponseBody runQuery(String queryId, QueryConfig queryConfig, Object params) {
        val queryContext = queryContext(queryId, queryConfig);

        return (outputStream) -> duckDbDataSource.withConnection(streamResult(queryContext, queryConfig, outputStream), params);
    }

    private static StatementRunnable streamResult(QueryContext queryContext, QueryConfig queryConfig, OutputStream outputStream) {
        return (connexion, args) -> {
            val queryBuilder = queryContext.queryBuilder();
            val resultSetConvertor = queryContext.resultSetConvertor();

            val prepareQuery = queryBuilder.prepareQuery(connexion, queryConfig, args);
            ResultSetIterable<?> objects = prepareQuery.executeAndFetchLazy(resultSetConvertor::convert);
            objects.forEach(elt -> {
                String outLine = unsafeToJsonStr(elt, queryContext.jsonConvertor());
                byte[] bytes = outLine.getBytes(StandardCharsets.UTF_8);
                try {
                    outputStream.write(bytes);
                    outputStream.write('\n');
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }

    private QueryContext queryContext(String queryId, QueryConfig queryConfig) {
        return queryContextCacheMap.computeIfAbsent(queryId, (k) -> computeQueryContext(queryConfig));
    }

    private QueryContext computeQueryContext(QueryConfig queryConfig) {
        val queryBuilder = applicationContext.getBean(queryConfig.getQueryBuilderClass());
        val resultSetConvertor = applicationContext.getBean(queryConfig.getResultSetConvertorClass());
        val jsonConvertor = applicationContext.getBean(queryConfig.getJsonConvertorClass());
        return new QueryContext(queryBuilder, resultSetConvertor, jsonConvertor);
    }

    private static <T> String unsafeToJsonStr(Object data, JsonConvertor<T> converter) {
        return converter.toJsonStr((T) data);
    }

    private record QueryContext(
            QueryBuilder<?> queryBuilder,
            ResultSetConvertor<?> resultSetConvertor,
            JsonConvertor<?> jsonConvertor
    ){}
}
