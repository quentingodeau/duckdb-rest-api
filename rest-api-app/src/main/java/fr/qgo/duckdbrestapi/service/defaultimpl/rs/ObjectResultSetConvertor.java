package fr.qgo.duckdbrestapi.service.defaultimpl.rs;

import fr.qgo.duckdbrestapi.exception.InvalidPojo;
import fr.qgo.duckdbrestapi.reflection.PojoMetadata;
import fr.qgo.duckdbrestapi.service.ResultSetConvertor;
import org.duckdb.DuckDBArray;
import org.duckdb.DuckDBStruct;
import org.springframework.beans.DirectFieldAccessor;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.ResultSetHandlerFactory;
import org.sql2o.ResultSetIterable;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class ObjectResultSetConvertor<T> implements ResultSetConvertor<T> {
    private final PojoMetadata metadata;
    private final boolean throwOnMappingFailure;

    public ObjectResultSetConvertor(Class<T> clazz, boolean caseSensitive, boolean autoDeriveColumnNames, Map<String, String> columnMappings, boolean throwOnMappingFailure) {
        this.metadata = new PojoMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings);
        this.throwOnMappingFailure = throwOnMappingFailure;
    }

    @Override
    public ResultSetIterable<T> executeAndFetchLazy(Query query, Map<String, Object> userQueryParams) throws SQLException {
        return query.executeAndFetchLazy((ResultSetHandlerFactory<T>) meta -> {
            final int columnCount = meta.getColumnCount();
            final Field[] fields = new Field[columnCount + 1];   // setters[0] is always null;

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);

                fields[i] = metadata.getFieldIfExists(colName);

                // If more than 1 column is fetched (we cannot fall back to executeScalar),
                // and the setter doesn't exist, throw exception.
                if (throwOnMappingFailure && fields[i] == null && columnCount > 1) {
                    throw new InvalidPojo("Could not map " + colName + " to any property of class " + metadata.getClazz() + ".");
                }
            }

            return (ResultSetHandler<T>) resultSet -> {
                @SuppressWarnings("unchecked")
                T pojo = (T) metadata.newInstance();

                try {
                    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
                        Field field = fields[colIdx];
                        if (field == null) continue;
                        oMap(resultSet.getObject(colIdx), pojo, field);
                    }
                } catch (IllegalAccessException e) {
                    throw new InvalidPojo(e);
                }

                return pojo;
            };
        });
    }

    private void oMap(Object value, T pojo, Field field) throws SQLException, IllegalAccessException {
        if (value == null) {
            if (field.getType().isPrimitive()) {
                // Nothing to do can not set primitive to null
            } else {
                field.set(pojo, null);
            }
        } else if (value instanceof DuckDBArray array) {
            oMap(array, pojo, field);
        } else if (value instanceof DuckDBStruct struct) {
            oMap(struct, pojo, field);
        } else if (value instanceof Map<?, ?> map) {
            oMap(map, pojo, field);
        } else {
            field.set(pojo, value);
        }
    }

    private void oMap(Map<?, ?> map, T pojo, Field field) {
    }

    private void oMap(DuckDBArray array, T pojo, Field field) throws SQLException {
        Object[] elts = (Object[]) array.getArray();

        Class<?> type = field.getType();
        if (type.isArray()) {

        } else if ((type.getModifiers() & Modifier.INTERFACE) != 0) {
            if (Set.class.isAssignableFrom(type)) {

            } else if (List.class.isAssignableFrom(type)) {
                List<?> l = new ArrayList<>(elts.length);

            } else if (Map.class.isAssignableFrom(type)) {

            }
        } else if ((type.getModifiers() & Modifier.ABSTRACT) != 0) {

        } else {

        }
    }

    private void oMap(DuckDBStruct struct, T pojo, Field setter) {

    }
}
