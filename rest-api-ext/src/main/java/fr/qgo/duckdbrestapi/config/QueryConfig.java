package fr.qgo.duckdbrestapi.config;

import fr.qgo.duckdbrestapi.service.JsonConvertor;
import fr.qgo.duckdbrestapi.service.QueryBuilder;
import fr.qgo.duckdbrestapi.service.ResultSetConvertor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
public class QueryConfig {
    public enum ExpositionVerb {POST} // TODO allow get

    private ExpositionVerb verb = ExpositionVerb.POST;
    private String query;
    private Class<?> payloadClass = void.class;
    private Map<String, PojoFieldType> payloadStruct;
    private Class<? extends QueryBuilder> queryBuilderClass;
    private Class<? extends ResultSetConvertor<?>> resultSetConvertorClass;
    private Class<? extends JsonConvertor<?>> jsonConvertorClass;
    private QueryDoc doc;
    private Map<String, Object> userQueryParams;
}