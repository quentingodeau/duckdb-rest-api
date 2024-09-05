package fr.qgo.duckdbrestapi.service;

import java.util.Map;
import java.util.Properties;

public interface JsonConvertor<T> {
    String toJsonStr(T data, Map<String, Object> userQueryParams);
}
