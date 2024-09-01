package fr.qgo.duckdbrestapi.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class QueryDoc {
    private List<String> tags;
    private String summary;
    private String description;
    private Class<?> returnedType;
    private Map<String, PojoFieldType> returnedStruct;
}
