package fr.qgo.duckdbrestapi.service;

public interface JsonConvertor<T> {
    String toJsonStr(T data);
}
