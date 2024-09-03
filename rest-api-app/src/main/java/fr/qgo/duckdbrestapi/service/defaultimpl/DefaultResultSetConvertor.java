package fr.qgo.duckdbrestapi.service.defaultimpl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.qgo.duckdbrestapi.service.ResultSetConvertor;
import lombok.SneakyThrows;
import lombok.val;
import org.duckdb.DuckDBArray;
import org.duckdb.DuckDBStruct;
import org.springframework.stereotype.Service;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.ResultSetIterable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

@Service
public class DefaultResultSetConvertor implements ResultSetConvertor<String> {
    private final ObjectWriter resultSetWriter;

    public DefaultResultSetConvertor(ObjectMapper om) {
        val objectMapper = om.copy();
        objectMapper.disable(SerializationFeature.CLOSE_CLOSEABLE); // We do not want to close the result set!
        SimpleModule duckDbModule = new SimpleModule("DuckDbResultSet", new Version(1, 0, 0, null, "groupId", "artefactId"));// TODO
        duckDbModule.addSerializer(ResultSet.class, new ResultSetSerializer());
        duckDbModule.addSerializer(DuckDBStruct.class, new DuckDBStructSerializer());
        duckDbModule.addSerializer(DuckDBArray.class, new DuckDBArraySerializer());
        objectMapper.registerModule(duckDbModule);
        this.resultSetWriter = objectMapper.writerFor(ResultSet.class);
    }

    @Override
    public ResultSetIterable<String> executeAndFetchLazy(Query query, Properties userQueryParams) throws SQLException {
        return query.executeAndFetchLazy((ResultSetHandler<String>) rs -> {
            try {
                return resultSetWriter.writeValueAsString(rs);
            } catch (JsonProcessingException e) {
                throw new SQLException(e);
            }
        });
    }

    private static final class ResultSetSerializer extends JsonSerializer<ResultSet> {
        @Override
        @SneakyThrows(SQLException.class)
        public void serialize(ResultSet resultSet, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int numColumns = metaData.getColumnCount();

            jgen.writeStartObject();
            for (int i = 1; i <= numColumns; i++) {
                String columnName = metaData.getColumnName(i);
                Object object = resultSet.getObject(i);

                jgen.writeFieldName(columnName);
                jgen.writeObject(object);
            }
            jgen.writeEndObject();

        }
    }

    private static final class DuckDBStructSerializer extends JsonSerializer<DuckDBStruct> {
        @Override
        @SneakyThrows(SQLException.class)
        public void serialize(DuckDBStruct struct, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
            Map<String, Object> map = struct.getMap();

            jgen.writeStartObject();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                jgen.writeFieldName(entry.getKey());
                jgen.writeObject(entry.getValue());
            }
            jgen.writeEndObject();
        }
    }

    private static final class DuckDBArraySerializer extends JsonSerializer<DuckDBArray> {
        @Override
        @SneakyThrows(SQLException.class)
        public void serialize(DuckDBArray array, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
            Object[] elts = (Object[]) array.getArray();

            jgen.writeStartArray();
            for (Object elt : elts) {
                jgen.writeObject(elt);
            }
            jgen.writeEndArray();
        }
    }
}
