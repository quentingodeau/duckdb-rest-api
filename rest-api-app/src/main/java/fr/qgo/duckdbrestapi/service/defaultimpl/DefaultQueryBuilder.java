package fr.qgo.duckdbrestapi.service.defaultimpl;

import fr.qgo.duckdbrestapi.config.QueryConfig;
import fr.qgo.duckdbrestapi.service.QueryBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Query;

import java.sql.SQLException;
import java.util.Map;


@Slf4j
@Service
@AllArgsConstructor
public final class DefaultQueryBuilder implements QueryBuilder<JSONObject> {
    @Override
    public Query prepareQuery(Connection connection, QueryConfig query, Object params) throws SQLException {
        Query comQuery = connection.createQuery(query.getQuery(), false);
        if (params instanceof Map payload) {
            ((Map<String, ?>) payload).forEach(comQuery::addParameter);
            return comQuery;
        } else {
            return comQuery.bind(params);
        }
    }
}
