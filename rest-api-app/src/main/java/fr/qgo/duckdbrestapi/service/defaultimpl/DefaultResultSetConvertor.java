package fr.qgo.duckdbrestapi.service.defaultimpl;

import fr.qgo.duckdbrestapi.service.ResultSetConvertor;
import org.duckdb.DuckDBArray;
import org.duckdb.DuckDBStruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

@Service
public class DefaultResultSetConvertor implements ResultSetConvertor<JSONObject> {
    @Override
    public JSONObject convert(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int numColumns = metaData.getColumnCount();
        JSONObject result = new JSONObject();

        for (int i = 1; i <= numColumns; i++) {
            String columnName = metaData.getColumnName(i);
            Object object = resultSet.getObject(i);
            Object value = tryConvertObject(object);
            result.put(columnName, value);

        }
        return result;
    }

    private JSONObject convert(DuckDBStruct object) throws SQLException {
        Map<String, Object> map = object.getMap();
        JSONObject result = new JSONObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Object finalValue = tryConvertObject(entry.getValue());
            result.put(entry.getKey(), finalValue);
        }
        return result;
    }

    private JSONArray convert(DuckDBArray object) throws SQLException {
        Object[] elts = (Object[]) object.getArray();
        JSONArray array = new JSONArray(elts.length);

        for (Object elt : elts) {
            array.put(tryConvertObject(elt));
        }

        return array;
    }

    private Object tryConvertObject(Object value) throws SQLException {
        final Object finalValue;
        if (value == null) {
            finalValue = JSONObject.NULL;
        } else if (value instanceof DuckDBArray array) {
            finalValue = convert(array);
        } else if (value instanceof DuckDBStruct struct) {
            finalValue = convert(struct);
        } else {
            finalValue = value;
        }
        return finalValue;
    }
}
