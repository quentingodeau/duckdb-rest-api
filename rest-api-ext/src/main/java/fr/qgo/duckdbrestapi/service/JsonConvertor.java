package fr.qgo.duckdbrestapi.service;

import java.util.Properties;

public interface JsonConvertor<T> {
    String toJsonStr(T data, Properties userQueryParams);
}
